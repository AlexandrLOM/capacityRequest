package tech.scalea.capacityRequest.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tech.scalea.capacityRequest.entity.InvCapacityRequestEntity;
import tech.scalea.capacityRequest.entity.InvCapacityRequestItemEntity;
import tech.scalea.capacityRequest.entity.ReportHostStatic;
import tech.scalea.capacityRequest.entity.THostEntity;
import tech.scalea.capacityRequest.model.CalculationData;
import tech.scalea.capacityRequest.model.ServerModel;
import tech.scalea.capacityRequest.model.VmModel;
import tech.scalea.capacityRequest.model.response.ServerInfo;
import tech.scalea.capacityRequest.repository.CapacityRequestItemRepository;
import tech.scalea.capacityRequest.repository.CapacityRequestRepository;
import tech.scalea.capacityRequest.repository.InvVmRepository;
import tech.scalea.capacityRequest.repository.ReportHostStaticRepository;
import tech.scalea.capacityRequest.repository.THostRepository;
import tech.scalea.capacityRequest.service.DataPreparationService;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class DataPreparationServiceImpl implements DataPreparationService {

    private static final Logger logger = LoggerFactory.getLogger(DataPreparationServiceImpl.class);

    @Value("${app.capacityReserve}")
    private double capacityReserve;

    private CapacityRequestItemRepository capacityRequestItemRepository;
    private ReportHostStaticRepository reportHostStaticRepository;
    private InvVmRepository invVmRepository;
    private CapacityRequestRepository capacityRequestRepository;
    private THostRepository tHostRepository;

    @Autowired
    public DataPreparationServiceImpl(CapacityRequestItemRepository capacityRequestItemRepository,
                                      ReportHostStaticRepository reportHostStaticRepository,
                                      CapacityRequestRepository capacityRequestRepository,
                                      THostRepository tHostRepository,
                                      InvVmRepository invVmRepository) {
        this.capacityRequestItemRepository = capacityRequestItemRepository;
        this.reportHostStaticRepository = reportHostStaticRepository;
        this.invVmRepository = invVmRepository;
        this.capacityRequestRepository = capacityRequestRepository;
        this.tHostRepository = tHostRepository;

    }

    @Override
    public List<VmModel> getVmModelListByCapacityRequestId(UUID id) {
        return toVmModelList(capacityRequestItemRepository.findAllByInvCapacityRequestEntity_Id(id));
    }

    @Override
    public CalculationData getVmModelListByCapacityRequestIdAndAllVMsBefore(UUID id) {
        CalculationData data = new CalculationData();
        data.setInvCapacityRequestEntityList(new ArrayList<>());
        data.setVmModelList(new ArrayList<>());

        Optional<InvCapacityRequestEntity> invCapacityRequestEntityOptional = capacityRequestRepository.findById(id);
        if (!invCapacityRequestEntityOptional.isPresent()) {
            logger.warn("Not found capacity request by ID: {}", id);
            return data;
        }
        data.setDueDate(invCapacityRequestEntityOptional.get().getDueDate());
        data.getInvCapacityRequestEntityList().add(invCapacityRequestEntityOptional.get());
        logger.info("Capacity request Id [{}], due date: {}",
                id,
                invCapacityRequestEntityOptional.get().getDueDate());

        data.getVmModelList().addAll(getVmModelListByCapacityRequestId(invCapacityRequestEntityOptional.get().getId()));
        logger.debug("Capacity request Id [{}], VMs: {}",
                invCapacityRequestEntityOptional.get().getId(),
                data.getVmModelList().size());

        List<InvCapacityRequestEntity> invCapacityRequestEntityList = capacityRequestRepository.findAllByDueDateBefore(
                invCapacityRequestEntityOptional.get().getDueDate());

        for (InvCapacityRequestEntity capacityRequestEntity : invCapacityRequestEntityList) {
            data.getInvCapacityRequestEntityList().add(capacityRequestEntity);
            List<VmModel> vmModels = getVmModelListByCapacityRequestId(capacityRequestEntity.getId());
            data.getVmModelList().addAll(vmModels);
            logger.debug("Included in calculation. Capacity request Id [{}], VMs: {}",
                    capacityRequestEntity.getId(),
                    vmModels.size());
        }
        return data;
    }

    public List<VmModel> toVmModelList(List<InvCapacityRequestItemEntity> entityList) {
        List<VmModel> vmModelList = new ArrayList<>();

        for (InvCapacityRequestItemEntity entity : entityList) {
            if (entity.getAffinityGroup() != null && !entity.getAffinityGroup().isEmpty()) {
                vmModelList.add(toVmModel(entity));
            } else {
                vmModelList.addAll(getVmModelGroup(entity));
            }
        }
        return vmModelList;
    }

    @Override
    public ServerInfo getServerInfo(String type){
        ServerInfo serverInfo = new ServerInfo();
//        Optional<THostEntity> entityOptional = tHostRepository.findByHostType(type);
        List<THostEntity> tHostEntities = tHostRepository.findAllByHostType(type);
        Optional<THostEntity> entityOptional = tHostEntities.stream().findFirst();
        if(entityOptional.isPresent()){
            serverInfo.setType(type);
            serverInfo.setVCpu((int)Math.round(entityOptional.get().getCpuQty() * capacityReserve));
            serverInfo.setRam((int)Math.round(entityOptional.get().getRamCapacity() * capacityReserve));
        } else {
            logger.warn("Not server template found for type: [{}]", type);
        }
        return serverInfo;
    }

//    public List<List<VmModel>> sortVmsByDcAndType(List<VmModel> vmModelList){
//        List<List<VmModel>> sortedVms = new ArrayList<>();
//        Set<String> dcIdSet = vmModelList.stream().map(VmModel::getDcId).collect(Collectors.toSet());
//    }


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
    public List<ServerModel> getServerModelList() {
        Date lastDate = reportHostStaticRepository.findLastDate();
        return toServerModelList(reportHostStaticRepository.findAllByTimestamp(lastDate));
    }


    @Override
    public List<String> getDcIdListAllCapacityRequestItems() {
        return capacityRequestItemRepository.findDcIdList();
    }

    @Override
    public List<VmModel> getVmModelListByDcId(String dcId) {
        return toVmModelList(capacityRequestItemRepository.findAllByDcId(dcId));
    }

    @Override
    public List<InvCapacityRequestEntity> getInvCapacityRequestEntityList() {
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

    public List<ServerModel> toServerModelList(List<ReportHostStatic> entityList) {
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
            if (entity.getVcpuQty() == null) {
                logger.warn("Missing value vCPU for {}", entity);
                return 0;
            }
            if (entity.getVmQty() == null) {
                logger.warn("Missing value quantity VMs for {}", entity);
                return entity.getVmQty();
            }
            return entity.getVcpuQty() * entity.getVmQty();
        } else {
            if (entity.getVcpuQty() == null) {
                logger.warn("Missing value vCPU for {}", entity);
                return 0;
            }
            return entity.getVcpuQty();
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
            model.setVCpuQuantity((int)Math.round(entity.getTotalCpuQty() * capacityReserve) - entity.getAllocCpuQty());
        } else {
            logger.warn("Missing value TotalCpuQty or AllocCpuQty for {}", entity);
            model.setVCpuQuantity(0);
        }
        if (entity.getTotalCpuQty() != null && entity.getAllocCpuQty() != null) {
            model.setRamQuantity((int)Math.round(entity.getTotalRamQty() * capacityReserve) - entity.getAllocRamQty().intValue());
        } else {
            logger.warn("Missing value TotalRamQty or AllocRamQty for {}", entity);
            model.setVCpuQuantity(0);
        }

        model.setVmCount(invVmRepository.countByHostId(entity.getHostId()));

        return model;
    }


}
