package tech.scalea.capacityRequest.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tech.scalea.capacityRequest.entity.InvCapacityRequestEntity;
import tech.scalea.capacityRequest.entity.InvCapacityRequestItemEntity;
import tech.scalea.capacityRequest.entity.InvExpansionRequestItemEntity;
import tech.scalea.capacityRequest.entity.ReportHostStatic;
import tech.scalea.capacityRequest.entity.ReportStorageStatic;
import tech.scalea.capacityRequest.entity.THostEntity;
import tech.scalea.capacityRequest.model.CalculationData;
import tech.scalea.capacityRequest.model.ServerModel;
import tech.scalea.capacityRequest.model.VmModel;
import tech.scalea.capacityRequest.model.response.ServerInfo;
import tech.scalea.capacityRequest.repository.CapacityRequestItemRepository;
import tech.scalea.capacityRequest.repository.CapacityRequestRepository;
import tech.scalea.capacityRequest.repository.ExpansionRequestItemRepository;
import tech.scalea.capacityRequest.repository.InvVmRepository;
import tech.scalea.capacityRequest.repository.ReportHostStaticRepository;
import tech.scalea.capacityRequest.repository.ReportStorageStaticRepository;
import tech.scalea.capacityRequest.repository.THostRepository;
import tech.scalea.capacityRequest.service.DataPreparationService;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
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
    private ExpansionRequestItemRepository expansionRequestItemRepository;
    private ReportStorageStaticRepository reportStorageStaticRepository;

    @Autowired
    public DataPreparationServiceImpl(CapacityRequestItemRepository capacityRequestItemRepository,
                                      ReportHostStaticRepository reportHostStaticRepository,
                                      CapacityRequestRepository capacityRequestRepository,
                                      THostRepository tHostRepository,
                                      ExpansionRequestItemRepository expansionRequestItemRepository,
                                      ReportStorageStaticRepository reportStorageStaticRepository,
                                      InvVmRepository invVmRepository) {
        this.capacityRequestItemRepository = capacityRequestItemRepository;
        this.reportHostStaticRepository = reportHostStaticRepository;
        this.invVmRepository = invVmRepository;
        this.capacityRequestRepository = capacityRequestRepository;
        this.expansionRequestItemRepository = expansionRequestItemRepository;
        this.reportStorageStaticRepository = reportStorageStaticRepository;
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

        Date dueDate = formatDate(invCapacityRequestEntityOptional.get().getDueDate(), 1);
        Date fromDate = formatDate(new Date(), 0);

        data.setDueDate(dueDate);
        data.setFromDate(fromDate);
        data.getInvCapacityRequestEntityList().add(invCapacityRequestEntityOptional.get());
        logger.info("Capacity request Id [{}], due date: {}",
                id,
                dueDate);

        data.getVmModelList().addAll(getVmModelListByCapacityRequestId(invCapacityRequestEntityOptional.get().getId()));
        logger.debug("Capacity request Id [{}], VMs: {}",
                invCapacityRequestEntityOptional.get().getId(),
                data.getVmModelList().size());

        List<InvCapacityRequestEntity> invCapacityRequestEntityList = capacityRequestRepository.findAllByDueDateBeforeAndDueDateAfter(
                dueDate, fromDate);

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
    public ServerInfo getServerInfoFromTemplate(String type) {
        ServerInfo serverInfo = new ServerInfo();
//        Optional<THostEntity> entityOptional = tHostRepository.findByHostType(type);
        List<THostEntity> tHostEntities = tHostRepository.findAllByHostType(type);
        Optional<THostEntity> entityOptional = tHostEntities.stream().findFirst();
        if (entityOptional.isPresent()) {
            serverInfo.setType(type);
            serverInfo.setVCpu((int) Math.round(
                    (entityOptional.get().getCpuQty() == null ? 0 : entityOptional.get().getCpuQty())
                            * capacityReserve));
            serverInfo.setRam((int) Math.round(
                    (entityOptional.get().getRamCapacity() == null ? 0 : entityOptional.get().getRamCapacity())
                            * capacityReserve));
            serverInfo.setStorage((int) Math.round(
                    (entityOptional.get().getHddCapacity() == null ? 0 : entityOptional.get().getHddCapacity())
                            * capacityReserve));
        } else {
            //logger.warn("Not server template found for type: [{}]", type);
            throw new IllegalArgumentException("Not server template found for type: " + type);
        }
        return serverInfo;
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
    public List<ServerModel> getServerModelListByDcIdAndComputeType(String dcId, String computeType) {
        Date lastDate = reportHostStaticRepository.findLastDate();
        return toServerModelList(reportHostStaticRepository.findAllByDcIdAndHostTypeAndTimestamp(dcId, computeType, lastDate));
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
        model.setStorageQty(countStorageQty(entity));
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

    private int countStorageQty(InvCapacityRequestItemEntity entity) {
        return (entity.getLocalStorageQty() == null ? 0 : entity.getLocalStorageQty())
                * (entity.getVmQty() == null ? 0 : entity.getVmQty());
    }

    public ServerModel toServerModel(ReportHostStatic entity) {
        ServerModel model = new ServerModel();

        model.setDcId(entity.getDcId());
        model.setHostId(entity.getHostId());
        model.setHostIdLong(entity.getId());
        model.setHostName(entity.getHostName());
        model.setHostType(entity.getHostType());
        if (entity.getTotalCpuQty() != null && entity.getAllocCpuQty() != null) {
            model.setVCpuQuantity((int) Math.round(entity.getTotalCpuQty() * capacityReserve) - entity.getAllocCpuQty());
        } else {
            logger.warn("Missing value TotalCpuQty or AllocCpuQty for {}", entity);
            model.setVCpuQuantity(0);
        }
        if (entity.getTotalCpuQty() != null && entity.getAllocCpuQty() != null) {
            model.setRamQuantity((int) Math.round(entity.getTotalRamQty() * capacityReserve) - entity.getAllocRamQty().intValue());
        } else {
            logger.warn("Missing value TotalRamQty or AllocRamQty for {}", entity);
            model.setVCpuQuantity(0);
        }

        model.setVmCount(invVmRepository.countByHostId(entity.getHostId()));

        return model;
    }

    @Override
    public List<ServerModel> getServerModelExpansionRequestByDueDateAndDcIdAndHostType(Date dueDate, Date fromDate, String dcId, String hostType) {
        List<InvExpansionRequestItemEntity> invExpansionRequestItemEntityList = expansionRequestItemRepository
                .findAllByInvExpansionRequestEntity_DueDateBeforeAndInvExpansionRequestEntity_DueDateAfterAndInvExpansionRequestEntity_InvDCAndHostType(
                        dueDate,
                        fromDate,
                        dcId,
                        hostType);
        List<ServerModel> serverModelList = new ArrayList<>();
        for (InvExpansionRequestItemEntity entity : invExpansionRequestItemEntityList) {
            int count = entity.getQuantity();
            while (count > 0) {
                ServerModel serverModel = new ServerModel();
                serverModel.setDcId(dcId);
                serverModel.setHostType(hostType);
                serverModel.setHostId(UUID.randomUUID().toString());
                serverModel.setVmCount(0);
                serverModel.setHostName("ExpansionRequest");
                serverModel.setVCpuQuantity((int) Math.round(entity.getCpuQty() * capacityReserve));
                serverModel.setRamQuantity((int) Math.round(entity.getRamCapacity() * capacityReserve));
                serverModelList.add(serverModel);

                count--;
            }
        }
        return serverModelList;
    }

    @Override
    public Double getStorageExpansionRequest(Date dueDate, Date fromDate, String dcId, String type) {
        List<InvExpansionRequestItemEntity> expansionRequestItemEntityList = expansionRequestItemRepository.findAllByInvExpansionRequestEntity_DueDateBeforeAndInvExpansionRequestEntity_DueDateAfterAndInvExpansionRequestEntity_InvDCAndHostType(
                dueDate,
                fromDate,
                dcId,
                type);
        double storageExpansionRequest = 0;
        for (InvExpansionRequestItemEntity entity : expansionRequestItemEntityList) {
            storageExpansionRequest = storageExpansionRequest
                    + (entity.getQuantity() == null ? 0 : entity.getQuantity())
                    * (entity.getHddCapacity() == null ? 0 : entity.getHddCapacity());
        }
        return storageExpansionRequest;
    }

    @Override
    public Double getStorageServers(String dcId, String type) {
        String typeServer = type;
        if (type.equals("STORAGE_SSD")) {
            typeServer = "SSD";
        } else if (type.equals("STORAGE_SAS")) {
            typeServer = "SAS";
        }
        Date date = reportStorageStaticRepository.findLastDateForDcId(dcId);
        List<ReportStorageStatic> reportStorageStaticsList = reportStorageStaticRepository.findAllByDcIdAndPoolTypeAndTimestamp(
                dcId,
                typeServer,
                date);
        double storageServers = 0;
        for (ReportStorageStatic entity : reportStorageStaticsList) {
            storageServers = storageServers +
                    (entity.getPoolTotal() == null && entity.getUsed() == null ? 0
                            : (entity.getPoolTotal() * capacityReserve) - entity.getUsed());

        }
        return Math.round(storageServers * 10000) / 10000D;
    }

    public Date formatDate(Date date, int addDays) {
        Date newDate = date;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Calendar c = Calendar.getInstance();
        try {
            c.setTime(sdf.parse(sdf.format(newDate)));
            c.add(Calendar.DATE, addDays);
            newDate = c.getTime();
        } catch (ParseException e) {
            logger.debug("Date conversion error: {}", date);
        }
        return newDate;
    }

}
