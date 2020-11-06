package tech.scalea.capacityRequest.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tech.scalea.capacityRequest.entity.InvCapacityRequestEntity;
import tech.scalea.capacityRequest.entity.InvCapacityRequestItemEntity;
import tech.scalea.capacityRequest.entity.ReportHostStatic;
import tech.scalea.capacityRequest.model.ServerModel;
import tech.scalea.capacityRequest.model.VmModel;
import tech.scalea.capacityRequest.repository.CapacityRequestItemRepository;
import tech.scalea.capacityRequest.repository.CapacityRequestRepository;
import tech.scalea.capacityRequest.repository.InvVmRepository;
import tech.scalea.capacityRequest.repository.ReportHostStaticRepository;
import tech.scalea.capacityRequest.service.DataPreparationService;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class DataPreparationServiceImpl implements DataPreparationService {

    private static final Logger logger = LoggerFactory.getLogger(DataPreparationServiceImpl.class);

    private CapacityRequestItemRepository capacityRequestItemRepository;
    private ReportHostStaticRepository reportHostStaticRepository;
    private InvVmRepository invVmRepository;
    private CapacityRequestRepository capacityRequestRepository;

    @Autowired
    public DataPreparationServiceImpl(CapacityRequestItemRepository capacityRequestItemRepository,
                                      ReportHostStaticRepository reportHostStaticRepository,
                                      CapacityRequestRepository capacityRequestRepository,
                                      InvVmRepository invVmRepository) {
        this.capacityRequestItemRepository = capacityRequestItemRepository;
        this.reportHostStaticRepository = reportHostStaticRepository;
        this.invVmRepository = invVmRepository;
        this.capacityRequestRepository = capacityRequestRepository;

    }

    @Override
    public List<String> getDcIdListAllCapacityRequestItems() {
        return capacityRequestItemRepository.findDcIdList();
    }

    @Override
    public List<VmModel> getVmModelListByCapacityRequestId(UUID id) {
        return toVmModelList(capacityRequestItemRepository.findAllByInvCapacityRequestEntity_Id(id));
    }

    @Override
    public List<VmModel> getVmModelListByDcId(String dcId) {
        return toVmModelList(capacityRequestItemRepository.findAllByDcId(dcId));
    }

    public List<VmModel> toVmModelList(List<InvCapacityRequestItemEntity> entityList) {
        List<VmModel> vmModelList = new ArrayList<>();

        for (InvCapacityRequestItemEntity entity : entityList) {
            if(entity.getAffinityGroup() != null && !entity.getAffinityGroup().isEmpty()){
                vmModelList.add(toVmModel(entity));
            } else {
                vmModelList.addAll(getVmModelGroup(entity));
            }
        }
        return vmModelList;
    }

    private List<VmModel> getVmModelGroup(InvCapacityRequestItemEntity entity) {
        List<VmModel> vmModelList = new ArrayList<>();
        int vmQuantity = entity.getVmQty();
        while (vmQuantity > 0) {
            vmModelList.add(toVmModel(entity));
            vmQuantity--;
        }
        return vmModelList;
    }

    @Override
    public List<InvCapacityRequestEntity> getInvCapacityRequestEntityList(){
        return capacityRequestRepository.findByOrderByDueDate();
    }

    @Override
    public List<ServerModel> getServerModelListByDcIdAndComputeType(String dcId, String computeType) {
        Date lastDate = reportHostStaticRepository.findLastDateForDcId(dcId);
        return toServerModelList(reportHostStaticRepository.findAllByDcIdAndHostTypeAndTimestamp(dcId, computeType, lastDate));
    }

    @Override
    public List<ServerModel> getServerModelListByDcId(String dcId) {
        Date lastDate = reportHostStaticRepository.findLastDateForDcId(dcId);
        return toServerModelList(reportHostStaticRepository.findAllByDcIdAndTimestamp(dcId, lastDate));
    }

    @Override
    public List<ServerModel> getServerModelList() {
        Date lastDate = reportHostStaticRepository.findLastDate();
        return toServerModelList(reportHostStaticRepository.findAllByTimestamp(lastDate));
    }

    public List<ServerModel> toServerModelList(List<ReportHostStatic> entityList){
        List<ServerModel> serverModelList = new ArrayList<>();
        entityList.forEach(entity -> serverModelList.add(toServerModel(entity)));
        return serverModelList;
    }


    public VmModel toVmModel(InvCapacityRequestItemEntity entity) {
        VmModel model = new VmModel();

        model.setDcId(entity.getDcId());
        model.setVmName(entity.getVmName());
        model.setVmId(entity.getId());
        model.setAffinityGroup(entity.getAffinityGroup());
        model.setAntiAffinityGroup(entity.getAntiAffinityGroup());
        model.setVcpuQty(countVcpuQty(entity));
        model.setRamQty(countRamQty(entity));
        model.setDedicatedCompute(entity.getDedicatedCompute() != null && entity.getDedicatedCompute().contains("YES"));
        model.setComputeType(entity.getComputeType());

        return model;
    }

    private int countVcpuQty(InvCapacityRequestItemEntity entity) {
        if (entity.getAffinityGroup() != null && !entity.getAffinityGroup().isEmpty()) {
            if(entity.getVcpuQty() == null){
                logger.warn("Missing value vCPU for {}", entity);
                return 0;
            } if(entity.getVmQty() == null){
                logger.warn("Missing value quantity VMs for {}", entity);
                return entity.getVmQty();
            }
            return entity.getVcpuQty() * entity.getVmQty();
        } else {
            if(entity.getVcpuQty() == null){
                logger.warn("Missing value vCPU for {}", entity);
                return 0;
            }
            return  entity.getVcpuQty();
        }
    }

    private int countRamQty(InvCapacityRequestItemEntity entity) {
        if (entity.getAffinityGroup() == null || entity.getAffinityGroup().isEmpty()) {
            return entity.getRamQty() == null ? 0 : entity.getRamQty();
        } else {
            return (entity.getRamQty() == null ? 0 : entity.getRamQty())
                    * (entity.getVmQty() == null ? 0 : entity.getVmQty());
        }
    }

    public ServerModel toServerModel(ReportHostStatic entity) {
        ServerModel model = new ServerModel();

        model.setDcId(entity.getDcId());
        model.setHostId(entity.getHostId());
        model.setHostIdLong(entity.getId());
        model.setHostName(entity.getHostName());
        model.setHostType(entity.getHostType());
        if (entity.getTotalCpuQty() != null && entity.getAllocCpuQty() != null) {
            model.setVCpuQuantity(entity.getTotalCpuQty() - entity.getAllocCpuQty());
        } else {
            logger.warn("Missing value TotalCpuQty or AllocCpuQty for {}", entity);
            model.setVCpuQuantity(0);
        }
        if (entity.getTotalCpuQty() != null && entity.getAllocCpuQty() != null) {
            model.setRamQuantity(entity.getTotalRamQty() - entity.getAllocRamQty().intValue());
        } else {
            logger.warn("Missing value TotalRamQty or AllocRamQty for {}", entity);
            model.setVCpuQuantity(0);
        }

        model.setVmCount(invVmRepository.countByHostId(entity.getHostId()));

        return model;
    }


}
