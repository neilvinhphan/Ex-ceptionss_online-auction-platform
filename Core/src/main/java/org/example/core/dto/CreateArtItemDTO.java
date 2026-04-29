package org.example.core.dto;

import java.math.BigDecimal;

public class CreateArtItemDTO extends CreateItemRequestDTO {
    private String artist;
    private int creationYear;

    public CreateArtItemDTO(){
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public int getCreationYear() {
        return creationYear;
    }

    public void setCreationYear(int creationYear) {
        this.creationYear = creationYear;
    }
}
