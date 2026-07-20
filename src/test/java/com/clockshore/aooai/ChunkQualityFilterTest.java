package com.clockshore.aooai;

import com.clockshore.aooai.config.ChunkQualityProperties;
import com.clockshore.aooai.service.ChunkQualityFilter;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChunkQualityFilterTest {

    private final ChunkQualityFilter filter = new ChunkQualityFilter(new ChunkQualityProperties());

    @Test
    void keepsNormalBodyChunk() {
        String text = "Julianne Jill Stingray has been one-half of the bartending team at VA-11 HALL-A. "
                + "She talks with customers, listens to their stories, and gives the document enough "
                + "semantic context for retrieval and embedding.";

        assertThat(filter.isHighQuality(text)).isTrue();
    }

    @Test
    void filtersPdfGarbledSplitChunk() {
        String text = """
                F a
                v o
                r i
                t e
                D
                r ink
                H E
                A D
                B O
                D Y
                """;

        assertThat(filter.isHighQuality(text)).isFalse();
    }

    @Test
    void filtersPageNumberChunk() {
        assertThat(filter.isHighQuality("12")).isFalse();
        assertThat(filter.isHighQuality("Page 12")).isFalse();
    }

    @Test
    void filtersEmptyTextChunk() {
        assertThat(filter.isHighQuality("")).isFalse();
        assertThat(filter.isHighQuality("   \n\t  ")).isFalse();
        assertThat(filter.isHighQuality(null)).isFalse();
    }
}
