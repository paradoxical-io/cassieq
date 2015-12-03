package io.paradoxical.cassieq.factories;

import io.paradoxical.cassieq.dataAccess.interfaces.MessageRepository;
import io.paradoxical.cassieq.dataAccess.interfaces.MonotonicRepository;
import io.paradoxical.cassieq.dataAccess.interfaces.PointerRepository;
import io.paradoxical.cassieq.dataAccess.interfaces.QueueRepository;
import lombok.Data;

@Data
public class DataContext{
    private final MessageRepository messageRepository;

    private final MonotonicRepository monotonicRepository;

    private final PointerRepository pointerRepository;

    private final QueueRepository queueRepository;
}
