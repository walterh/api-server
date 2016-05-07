package com.llug.api.persistence.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

@Entity
@Table(name = "account_email")
public class EntityAccountEmail implements EntityObject {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    Long id;

    @Column(name = "email")
    String email;

    @Column(name = "status")
    String status;

    @Column(name = "time_created")
    Long timeCreated;

    @Column(name = "time_updated")
    Long timeUpdated;

    @Column(name = "account_id")
    Long accountId;

    // usually don't care to map this in when retrieving.
    //@OneToOne(mappedBy = "account")
    @Transient
    private EntityAccount account;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getTimeCreated() {
        return timeCreated;
    }

    public void setTimeCreated(Long timeCreated) {
        this.timeCreated = timeCreated;
    }

    public Long getTimeUpdated() {
        return timeUpdated;
    }

    public void setTimeUpdated(Long timeUpdated) {
        this.timeUpdated = timeUpdated;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public EntityAccount getAccount() {
        return account;
    }

    public void setAccount(EntityAccount account) {
        this.account = account;
    }

    @Override
    public String toString() {
        return String.format("[%s (%d)]", email, id);
    }
}
