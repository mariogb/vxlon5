package org.lonpe.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public abstract class AbstractDcLon implements Serializable{
        private Long id;

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    private String pkey;

    public String getPkey() {
        return this.pkey;
    }

    public void setPkey(String pkey) {
        this.pkey = pkey;
    }

    private LocalDateTime createdDate = LocalDateTime.now();

    public LocalDateTime getCreatedDate() {
        return this.createdDate;
    }

    public void setCreatedDate(LocalDateTime creaDateTime) {
        this.createdDate = creaDateTime;
    }

    private Boolean active = Boolean.TRUE;

    /**
     *
     * @return active
     */
    public Boolean getActive() {
        return this.active;
    }

    /**
     *
     * @param active
     */
    public void setActive(Boolean active) {
        this.active = active;
    }

    @Override
    public int hashCode() {
        int result = (int) (id ^ (id >>> 32));
        result = 31 * result + id.hashCode();
        result = 31 * result + pkey.hashCode();
        return result;
    }
}
