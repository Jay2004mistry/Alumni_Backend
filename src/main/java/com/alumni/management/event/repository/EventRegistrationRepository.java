package com.alumni.management.event.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.alumni.management.event.entity.EventRegistration;
import java.util.List;
import java.util.Optional;



public interface EventRegistrationRepository extends JpaRepository<EventRegistration, Long> {

    boolean existsByEvent_IdAndUser_Id(Long eventId, Long userId);

    List<EventRegistration> findByEvent_IdOrderByRegisteredAtAsc(Long eventId);

    Optional<EventRegistration> findByEvent_IdAndUser_Id(Long eventId, Long userId);

    void deleteByEvent_Id(Long eventId);

}