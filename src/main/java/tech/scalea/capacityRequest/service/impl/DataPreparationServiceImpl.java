package tech.scalea.capacityRequest.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tech.scalea.capacityRequest.entity.InvCapacityRequestItemEntity;
import tech.scalea.capacityRequest.entity.ReportHostStatic;
import tech.scalea.capacityRequest.model.ServerModel;
import tech.scalea.capacityRequest.model.VmModel;
import tech.scalea.capacityRequest.repository.CapacityRequestItemRepository;
import tech.scalea.capacityRequest.repository.InvVmRepository;
import tech.scalea.capacityRequest.repository.ReportHostStaticRepository;
import tech.scalea.capacityRequest.service.DataPreparationService;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class DataPreparationServiceImpl implements DataPreparationService {

    private static final Logger logger = LoggerFactory.getLogger(DataPreparationServiceImpl.class);

    private CapacityRequestItemRepository capacityRequestItemRepository;
    private ReportHostStaticRepository reportHostStaticRepository;
    private InvVmRepository invVmRepository;

    @Autowired
    public DataPreparationServiceImpl(CapacityRequestItemRepository capacityRequestItemRepository,
                                      ReportHostStaticRepository reportHostStaticRepository,
                                      InvVmRepository invVmRepository) {
        this.capacityRequestItemRepository = capacityRequestItemRepository;
        this.reportHostStaticRepository = reportHostStaticRepository;
        this.invVmRepository = invVmRepository;
    }

    @Override
    public List<VmModel> getVmModelList(String dcId) {
        List<VmModel> vmModelList = new ArrayList<>();
        List<InvCapacityRequestItemEntity> entityList = capacityRequestItemRepository.findAllByDcId(dcId);
        entityList.forEach(entity -> vmModelList.add(toVmModel(entity)));
        return vmModelList;
    }


    @Override
    public List<ServerModel> getServerModelList(String dcId) {
        List<ServerModel> serverModelList = new ArrayList<>();

        Date lastDate = reportHostStaticRepository.findLastDateForDcId(dcId);
        List<ReportHostStatic> entityList = reportHostStaticRepository.findAllByDcIdAndTimestamp(dcId, lastDate);
        entityList.forEach(entity -> serverModelList.add(toServerModel(entity)));

        return serverModelList;
    }


    public VmModel toVmModel(InvCapacityRequestItemEntity entity) {
        VmModel model = new VmModel();

        model.setDcId(entity.getDcId());
        model.setVmName(entity.getVmName());
        model.setVmId(entity.getId());
        model.setAffinityGroup(entity.getAffinityGroup());
        model.setVcpuQty(countVcpuQty(entity));
        model.setRamQty(countRamQty(entity));
        model.setDedicatedCompute(entity.getDedicatedCompute() != null && entity.getDedicatedCompute().contains("YES"));
        model.setComputeType(entity.getComputeType());

        return model;
    }

    private int countVcpuQty(InvCapacityRequestItemEntity entity) {
        if (entity.getAffinityGroup() != null && !entity.getAffinityGroup().isEmpty()) {
            if (entity.getVmQty() != null) {
                return entity.getVcpuQty() * entity.getVmQty();
            } else {
                logger.warn("Missing value VmQty for {}", entity);
            }
        }
        return entity.getVcpuQty();
    }

    private int countRamQty(InvCapacityRequestItemEntity entity) {
        if (entity.getAffinityGroup() != null && !entity.getAffinityGroup().isEmpty()) {
            if (entity.getVmQty() != null) {
                return entity.getRamQty() * entity.getVmQty();
            } else {
                logger.warn("Missing value ramQty for {}", entity);
            }
        }
        return entity.getRamQty();
    }

    public ServerModel toServerModel(ReportHostStatic entity){
        ServerModel model = new ServerModel();

        model.setDcId(entity.getDcId());
        model.setHostId(entity.getHostId());
        model.setHostIdLong(entity.getId());
        model.setHostName(entity.getHostName());
        model.setHostType(entity.getHostType().getStringValue());
        if (entity.getTotalCpuQty() != null && entity.getAllocCpuQty() != null){
            model.setVCpuQuantity(entity.getTotalCpuQty() - entity.getAllocCpuQty());
        } else {
            logger.warn("Missing value TotalCpuQty or AllocCpuQty for {}", entity);
            model.setVCpuQuantity(0);
        }
        if (entity.getTotalCpuQty() != null && entity.getAllocCpuQty() != null){
            model.setRamQuantity(entity.getTotalRamQty() - entity.getAllocRamQty().intValue());
        } else {
            logger.warn("Missing value TotalRamQty or AllocRamQty for {}", entity);
            model.setVCpuQuantity(0);
        }

        model.setVmCount(invVmRepository.countByHostId(entity.getHostId()));

        return model;
    }
}
