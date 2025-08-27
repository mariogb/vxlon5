package org.lonpe.model;

import java.time.LocalDateTime;

public interface IDcLon {

    Long getId();

    String getPkey();

    void setId(Long id);

    void setPkey(String pkey);

    LocalDateTime getCreatedDate();

    void setCreatedDate(LocalDateTime createdDateTime);

    Boolean getActive();

    void setActive(Boolean active);

}
