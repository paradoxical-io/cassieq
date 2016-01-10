package io.paradoxical.cassieq.factories;

import io.paradoxical.cassieq.dataAccess.interfaces.MessageRepository;
import io.paradoxical.cassieq.dataAccess.interfaces.MonotonicRepository;
import io.paradoxical.cassieq.dataAccess.interfaces.PointerRepository;
import lombok.Data;

@Data
public class QueueDataContext {

    private final MessageRepository messageRepository;

    private final MonotonicRepository monotonicRepository;

    private final PointerRepository pointerRepository;
}
