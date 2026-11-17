package kr.ac.tukorea.ue.domain.interaction.dto;

import lombok.Getter;

public class FavoriteDto {

    @Getter
    public static class Response {
        private final boolean isFavorited;
        private final int favoriteCount;

        public Response(boolean isFavorited, int favoriteCount) {
            this.isFavorited = isFavorited;
            this.favoriteCount = favoriteCount;
        }
    }
}
