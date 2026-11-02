package com.example.gstrecon.repository;

import com.example.gstrecon.model.ReconRun;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReconRunRepository extends JpaRepository<ReconRun, Long> {
    List<ReconRun> findAllByOwnerUsername(String ownerUsername, org.springframework.data.domain.Sort sort);
}
