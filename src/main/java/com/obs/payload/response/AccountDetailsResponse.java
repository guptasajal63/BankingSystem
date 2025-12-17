package com.obs.payload.response;

import java.math.BigDecimal;

public class AccountDetailsResponse {
    private Long id;
    private String accountNumber;
    private BigDecimal balance;
    private boolean active;
    private String username;
    private Long userId;
    private String email;

    public AccountDetailsResponse(Long id, String accountNumber, BigDecimal balance, boolean active, String username, Long userId, String email) {
        this.id = id;
        this.accountNumber = accountNumber;
        this.balance = balance;
        this.active = active;
        this.username = username;
        this.userId = userId;
        this.email = email;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
