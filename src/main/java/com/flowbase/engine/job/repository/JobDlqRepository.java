package com.flowbase.engine.job.repository;

import com.flowbase.engine.job.domain.JobDlq;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobDlqRepository extends JpaRepository<JobDlq, String> {}
