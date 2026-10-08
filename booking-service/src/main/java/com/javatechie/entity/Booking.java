package com.javatechie.entity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Entity
@Data


public class Booking {
  @Id
  private Long id;
  private String bookingCode;
  private String showId;
  @ElementCollection
  private List<String> seatIds;
  private String userId;
  private String status; // PENDING, CONFIRMED, CANCELLED
  private Instant createdAt;
  private long amount;

  public Booking() {
  }
  public Booking(Long id, String bookingCode, String showId, List<String> seatIds, String userId, String status, Instant createdAt, long amount) {
    this.id = id;
    this.bookingCode = bookingCode;
    this.showId = showId;
    this.seatIds = seatIds;
    this.userId = userId;
    this.status = status;
    this.createdAt = createdAt;
    this.amount = amount;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getBookingCode() {
    return bookingCode;
  }

  public void setBookingCode(String bookingCode) {
    this.bookingCode = bookingCode;
  }

  public String getShowId() {
    return showId;
  }

  public void setShowId(String showId) {
    this.showId = showId;
  }

  public List<String> getSeatIds() {
    return seatIds;
  }

  public void setSeatIds(List<String> seatIds) {
    this.seatIds = seatIds;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public long getAmount() {
    return amount;
  }

  public void setAmount(long amount) {
    this.amount = amount;
  }

}
