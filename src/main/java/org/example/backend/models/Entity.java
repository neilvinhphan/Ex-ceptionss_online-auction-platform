package org.example.backend.models;

import java.time.LocalDateTime;

public abstract class Entity {
  protected int id;
  protected LocalDateTime createdAt = LocalDateTime.now();

  public Entity(int id) {
    this.id = id;
  }
}
