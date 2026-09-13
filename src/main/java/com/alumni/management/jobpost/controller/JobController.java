package com.alumni.management.jobpost.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alumni.management.jobpost.dto.JobDto;
import com.alumni.management.jobpost.entity.Job;
import com.alumni.management.jobpost.service.JobService;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

	@Autowired
	private JobService jobService;
	
	@PostMapping
	public String createJobpost(@RequestBody Job job) {
		return jobService.createJobpost(job);
	}
	
	
	@GetMapping()
	public List<JobDto> getAllJobs() {
		return jobService.getAllJobs();
	}

	@GetMapping("/my")
	public List<JobDto> getMyJobs() {
		return jobService.getMyJobs();
	}
	
	@PutMapping("/{id}")
	public String updateJob(@PathVariable Long id, @RequestBody JobDto jobDto) {
		return jobService.updateJob(id,jobDto);
	}
	
	@DeleteMapping("/{id}")
	public String deleteJob(@PathVariable Long id) {
		return jobService.deleteJob(id);
	}
	
}
