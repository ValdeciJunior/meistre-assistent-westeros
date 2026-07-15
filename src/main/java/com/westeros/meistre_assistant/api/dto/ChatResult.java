package com.westeros.meistre_assistant.api.dto;

import java.util.List;

public record ChatResult(String response, List<String> references) {}