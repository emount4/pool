package ru.mirea.pool.infrastructure.export;

import ru.mirea.pool.domain.model.Client;
import ru.mirea.pool.domain.model.Visit;

import java.nio.file.Path;
import java.util.List;

public interface DataExporter {

    void export(List<Client> clients, List<Visit> visits, Path destination);
}
