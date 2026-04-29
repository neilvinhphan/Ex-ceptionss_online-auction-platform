package org.example.core.models.entities;

import java.time.LocalDateTime;

public abstract class Entity {
  protected transient LocalDateTime createdAt;

  public Entity() {
    createdAt = LocalDateTime.now();
  }

  // Create User
  public Entity(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  // Setter & Getter
  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {this.createdAt = createdAt;}
}
