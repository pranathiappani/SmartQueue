package com.smartqueue.api.dto;

import java.util.List;
import java.util.Map;

public class NotificationPayload {
    private String userId;
    private String eventType;
    private List<String> channels;
    private Recipient recipient;
    private Map<String, String> templateData;

    public NotificationPayload() {}

    public static class Recipient {
        private String email;
        private String mobile;

        public Recipient() {}

        public Recipient(String email, String mobile) {
            this.email = email;
            this.mobile = mobile;
        }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getMobile() { return mobile; }
        public void setMobile(String mobile) { this.mobile = mobile; }
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public List<String> getChannels() { return channels; }
    public void setChannels(List<String> channels) { this.channels = channels; }
    public Recipient getRecipient() { return recipient; }
    public void setRecipient(Recipient recipient) { this.recipient = recipient; }
    public Map<String, String> getTemplateData() { return templateData; }
    public void setTemplateData(Map<String, String> templateData) { this.templateData = templateData; }
}
