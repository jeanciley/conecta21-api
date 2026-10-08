package br.com.conecta21.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;

public class GmudTemplateRequest {
    @NotBlank public String templateId = "gmud-time-aplicacao";
    @NotBlank public String operatorName;
    public Long operatorId;
    public String risksImpacts;
    public String requestDate;
    @NotBlank public String clientName;
    @NotBlank public String requester;
    @NotBlank public String ticket;
    @NotBlank public String classification;
    @NotBlank public String description;

    @Valid public Contact clientContact = new Contact();
    @Valid public Contact operatorContact = new Contact();
    public TasyDetails tasyDetails = new TasyDetails();

    public String environment = "PRODUÇÃO";
    public String operatingSystem = "LINUX";
    public String consoleAccess = "NÃO";
    public String interventionRequired = "SIM";

    public String executionDate;
    public String executionTime;
    public String totalExecutionTime;
    public String downtime;
    public String testTime;
    public String correctionTime;
    public String rollbackTime;

    public List<AgendaValue> agenda = new ArrayList<>();
    public List<ServiceAsset> services = new ArrayList<>();
    public List<Activity> testActivities = new ArrayList<>();
    public List<Activity> executionActivities = new ArrayList<>();
    public List<Activity> rollbackActivities = new ArrayList<>();
    public List<Risk> risks = new ArrayList<>();

    public static class Contact {
        @NotBlank public String name;
        public String phone;
        public String role;
        @Email public String email;
    }

    public static class TasyDetails {
        public Contact databaseContact = new Contact();
        public String tieStopDetails;
        public String tomcatIntegrationStopDetails;
        public String removedImages;
        public String addedImages;
        public String integrationArtifacts;
    }

    public static class AgendaValue {
        public String key;
        public String label;
        public String value;
    }

    public static class ServiceAsset {
        @NotBlank public String service;
        @NotBlank public String asset;
    }

    public static class Activity {
        @NotBlank public String activity;
        public String type;
        public String unavailability;
        public String time;
        @NotBlank public String responsible;
    }

    public static class Risk {
        public String risk;
        public String probability;
        public String impact;
        public String contingency;
    }
}
