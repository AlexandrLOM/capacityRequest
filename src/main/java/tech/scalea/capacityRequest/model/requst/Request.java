package tech.scalea.capacityRequest.model.requst;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class Request {

    public Date dueSate;
    public List<Setver> sererList;
}
