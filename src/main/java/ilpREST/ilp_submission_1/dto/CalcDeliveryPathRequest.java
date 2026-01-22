package ilpREST.ilp_submission_1.dto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.*;
public class CalcDeliveryPathRequest {
    @Valid
    @NotNull
    private List<MedDispatchRec> requests;
    public CalcDeliveryPathRequest() {}

    public CalcDeliveryPathRequest(List<MedDispatchRec> requests) {
        this.requests = requests;
    }

    public List<MedDispatchRec> getRequests() {
        return requests;
    }

    public void setRequests(List<MedDispatchRec> requests) {
        this.requests = requests;
    }

}
