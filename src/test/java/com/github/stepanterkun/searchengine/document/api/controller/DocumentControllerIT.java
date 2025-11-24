package com.github.stepanterkun.searchengine.document.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.stepanterkun.searchengine.common.exception.ErrorResponseDto;
import com.github.stepanterkun.searchengine.document.api.dto.DocumentDto;
import com.github.stepanterkun.searchengine.document.domain.model.Document;
import com.github.stepanterkun.searchengine.document.domain.model.DocumentStatus;
import com.github.stepanterkun.searchengine.document.domain.port.DocumentRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DocumentControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DocumentRepository documentRepository;

    @Test
    void create_shouldSaveToDbAndReturnDocument() throws Exception {
        Long ownerId = 42L;

        String body = """
            {
              "title": "Title 52",
              "content": "Content 52"
            }
            """;

        MvcResult mvcResult = mockMvc.perform(
                        post("/documents")
                                .header("X-User-Id", ownerId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                                      .andExpect(status().isCreated())
                                      .andReturn();

        String json = mvcResult.getResponse().getContentAsString();
        DocumentDto created = objectMapper.readValue(json, DocumentDto.class);

        assertThat(created).isNotNull();
        assertThat(created.id()).isNotNull();
        assertThat(created.title()).isEqualTo("Title 52");
        // service sets status to READY after successful indexing
        assertThat(created.status()).isEqualTo(DocumentStatus.READY);

        // extra check: ensure document actually persisted in db
        Document fromDb = documentRepository.findByIdAndOwnerId(created.id(), ownerId)
                                  .orElseThrow();

        assertThat(fromDb.getTitle()).isEqualTo("Title 52");
        assertThat(fromDb.getContent()).isEqualTo("Content 52");
        assertThat(fromDb.getOwnerId()).isEqualTo(ownerId);
        assertThat(fromDb.getStatus()).isEqualTo(DocumentStatus.READY);
    }

    @Test
    void create_whenTitleIsBlank_shouldReturn400WithValidationErrorBody() throws Exception {
        Long ownerId = 42L;

        String body = """
            {
              "title": "",
              "content": "Some content"
            }
            """;

        MvcResult mvcResult = mockMvc.perform(
                        post("/documents")
                                .header("X-User-Id", ownerId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                                      .andExpect(status().isBadRequest())
                                      .andReturn();

        String json = mvcResult.getResponse().getContentAsString();
        ErrorResponseDto error = objectMapper.readValue(json, ErrorResponseDto.class);

        assertThat(error.code()).isEqualTo("VALIDATION_ERROR");
        assertThat(error.message())
                .isNotBlank()
                .contains("must not be blank");
    }

    @Test
    void getAllForOwner_shouldReturnDocumentsForOwner() throws Exception {
        Long ownerId = 42L;

        Document doc1 = Document.newDocument("Title 1", "Content 1", ownerId);
        Document saved1 = documentRepository.save(doc1);

        Document doc2 = Document.newDocument("Title 2", "Content 2", ownerId);
        Document saved2 = documentRepository.save(doc2);

        Document other = Document.newDocument("Other title", "Other content", 999L);
        documentRepository.save(other);

        MvcResult mvcResult = mockMvc.perform(
                        get("/documents/all")
                                .header("X-User-Id", ownerId)
                )
                                      .andExpect(status().isOk())
                                      .andReturn();

        String json = mvcResult.getResponse().getContentAsString();
        DocumentDto[] dtos = objectMapper.readValue(json, DocumentDto[].class);

        assertThat(dtos)
                .isNotNull()
                .hasSize(2);

        assertThat(dtos)
                .extracting(DocumentDto::id)
                .containsExactlyInAnyOrder(saved1.getId(), saved2.getId());

        assertThat(dtos)
                .extracting(DocumentDto::title)
                .containsExactlyInAnyOrder("Title 1", "Title 2");
    }

    @Test
    void getAllForOwner_whenNoDocuments_shouldReturn404WithErrorBody() throws Exception {
        Long ownerId = 123L;

        MvcResult mvcResult = mockMvc.perform(
                        get("/documents/all")
                                .header("X-User-Id", ownerId)
                )
                                      .andExpect(status().isNotFound())
                                      .andReturn();

        String json = mvcResult.getResponse().getContentAsString();
        ErrorResponseDto error = objectMapper.readValue(json, ErrorResponseDto.class);

        assertThat(error.code()).isEqualTo("DOCUMENT_NOT_FOUND");
        assertThat(error.message())
                .isNotBlank()
                .contains("User does not have any documents loaded");
    }

    @Test
    void getByIdForOwner_whenDocumentExists_shouldReturnDocument() throws Exception {
        Long ownerId = 42L;

        Document doc = Document.newDocument("Title 1", "Content 1", ownerId);
        Document saved = documentRepository.save(doc);

        MvcResult mvcResult = mockMvc.perform(
                        get("/documents/{id}", saved.getId())
                                .header("X-User-Id", ownerId)
                )
                                      .andExpect(status().isOk())
                                      .andReturn();

        String json = mvcResult.getResponse().getContentAsString();
        DocumentDto dto = objectMapper.readValue(json, DocumentDto.class);

        assertThat(dto).isNotNull();
        assertThat(dto.id()).isEqualTo(saved.getId());
        assertThat(dto.title()).isEqualTo("Title 1");
        assertThat(dto.status()).isEqualTo(saved.getStatus());
    }

    @Test
    void getByIdForOwner_whenDocumentNotFound_shouldReturn404WithErrorBody() throws Exception {
        Long missingId = 999L;
        Long ownerId = 42L;

        MvcResult mvcResult = mockMvc.perform(
                        get("/documents/{id}", missingId)
                                .header("X-User-Id", ownerId)
                )
                                      .andExpect(status().isNotFound())
                                      .andReturn();

        String json = mvcResult.getResponse().getContentAsString();
        ErrorResponseDto error = objectMapper.readValue(json, ErrorResponseDto.class);

        assertThat(error.code()).isEqualTo("DOCUMENT_NOT_FOUND");
        assertThat(error.message())
                .isNotBlank()
                .contains(String.valueOf(missingId));
    }
}
