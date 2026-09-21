package com.alumni.management.admin.dto;

public class AdminStatsDto {
    private long totalUsers;
    private long totalAlumni;
    private long totalFaculty;
    private long totalStudents;
    private long totalJobs;
    private long activeJobs;
    private long totalEvents;
    private long activeEvents;

    public AdminStatsDto() {
    }

    public AdminStatsDto(long totalUsers, long totalAlumni, long totalFaculty, long totalStudents,
                         long totalJobs, long activeJobs, long totalEvents, long activeEvents) {
        this.totalUsers = totalUsers;
        this.totalAlumni = totalAlumni;
        this.totalFaculty = totalFaculty;
        this.totalStudents = totalStudents;
        this.totalJobs = totalJobs;
        this.activeJobs = activeJobs;
        this.totalEvents = totalEvents;
        this.activeEvents = activeEvents;
    }

    public long getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(long totalUsers) {
        this.totalUsers = totalUsers;
    }

    public long getTotalAlumni() {
        return totalAlumni;
    }

    public void setTotalAlumni(long totalAlumni) {
        this.totalAlumni = totalAlumni;
    }

    public long getTotalFaculty() {
        return totalFaculty;
    }

    public void setTotalFaculty(long totalFaculty) {
        this.totalFaculty = totalFaculty;
    }

    public long getTotalStudents() {
        return totalStudents;
    }

    public void setTotalStudents(long totalStudents) {
        this.totalStudents = totalStudents;
    }

    public long getTotalJobs() {
        return totalJobs;
    }

    public void setTotalJobs(long totalJobs) {
        this.totalJobs = totalJobs;
    }

    public long getActiveJobs() {
        return activeJobs;
    }

    public void setActiveJobs(long activeJobs) {
        this.activeJobs = activeJobs;
    }

    public long getTotalEvents() {
        return totalEvents;
    }

    public void setTotalEvents(long totalEvents) {
        this.totalEvents = totalEvents;
    }

    public long getActiveEvents() {
        return activeEvents;
    }

    public void setActiveEvents(long activeEvents) {
        this.activeEvents = activeEvents;
    }
}
