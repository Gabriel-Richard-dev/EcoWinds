package br.com.ecowinds.service.sigeho;

import br.com.ecowinds.model.ScheduleImport;

public record ImportOutcome(ScheduleImport record, String message) {}
