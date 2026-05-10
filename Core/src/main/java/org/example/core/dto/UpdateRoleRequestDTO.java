package org.example.core.dto;

public class UpdateRoleRequestDTO {
    private int userId;

    public UpdateRoleRequestDTO(int userId) {
        this.userId = userId;
    }

    public int getUserId() {
        return userId;
    }
}
