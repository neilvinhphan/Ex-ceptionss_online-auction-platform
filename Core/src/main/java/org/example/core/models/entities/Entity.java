package org.example.core.models.entities;

import java.time.LocalDateTime;

public abstract class Entity {
  protected transient LocalDateTime createdAt;

  public Entity() {
    createdAt = LocalDateTime.now();
  }

  public Entity(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }
}
