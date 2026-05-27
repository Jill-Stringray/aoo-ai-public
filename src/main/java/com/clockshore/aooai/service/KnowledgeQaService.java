package com.clockshore.aooai.service;

import com.clockshore.aooai.dto.AskRequest;
import com.clockshore.aooai.dto.AskResponse;
import reactor.core.publisher.Flux;

public interface KnowledgeQaService {
    AskResponse ask(AskRequest request);

    Flux<String> askStream(AskRequest request);
}
