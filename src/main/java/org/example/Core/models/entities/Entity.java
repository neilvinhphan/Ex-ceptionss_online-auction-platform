package org.example.core.models.entities;

import java.time.LocalDateTime;

public abstract class Entity {
  protected int id;
  protected LocalDateTime createdAt;

  public Entity() {
    createdAt = LocalDateTime.now();
  }

  // Create User
  public Entity(int id, LocalDateTime createdAt) {
    this.id = id;
    this.createdAt = createdAt;
  }

  // Create Item
  public Entity(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

    // Setter & Getter
  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }
}
