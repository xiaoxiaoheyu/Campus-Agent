package com.example.appbackend.controller;

import com.example.appbackend.entity.InterviewJobTechStack;
import com.example.appbackend.entity.Result;
import com.example.appbackend.repository.InterviewJobTechStackRepository;
import com.example.appbackend.util.InterviewJsonHelper;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/interview/job-positions")
public class AdminInterviewJobPositionController {

    private final InterviewJobTechStackRepository repository;

    public AdminInterviewJobPositionController(InterviewJobTechStackRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public Result<?> list() {
        List<Map<String, Object>> items = repository.findAll().stream().map(this::toMap).toList();
        return Result.success(Map.of("items", items));
    }

    @GetMapping("/{id}")
    public Result<?> detail(@PathVariable Long id) {
        return repository.findById(id)
                .<Result<?>>map(j -> Result.success(toMap(j)))
                .orElseGet(() -> Result.notFound("not found"));
    }

    @PostMapping
    public Result<?> create(@RequestBody Map<String, Object> body) {
        InterviewJobTechStack j = new InterviewJobTechStack();
        apply(j, body);
        j = repository.save(j);
        return Result.success(toMap(j));
    }

    @PutMapping("/{id}")
    public Result<?> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        InterviewJobTechStack j = repository.findById(id).orElse(null);
        if (j == null) return Result.notFound("not found");
        apply(j, body);
        j = repository.save(j);
        return Result.success(toMap(j));
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        if (!repository.existsById(id)) return Result.notFound("not found");
        repository.deleteById(id);
        return Result.success(Map.of("ok", true));
    }

    private void apply(InterviewJobTechStack j, Map<String, Object> body) {
        if (body.containsKey("job_position") || j.getJobPosition() == null) {
            j.setJobPosition(InterviewJsonHelper.asStr(body.get("job_position")));
        }
        if (body.containsKey("tech_stack") || j.getTechStack() == null) {
            j.setTechStack(InterviewJsonHelper.asStr(body.get("tech_stack")));
        }
        if (body.containsKey("status")) {
            j.setStatus(InterviewJsonHelper.asInt(body.get("status"), 1));
        }
        if (body.containsKey("remark")) {
            j.setRemark(InterviewJsonHelper.asStr(body.get("remark")));
        }
        if (j.getStatus() == null) j.setStatus(1);
    }

    private Map<String, Object> toMap(InterviewJobTechStack j) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", j.getId());
        m.put("job_position", j.getJobPosition());
        m.put("tech_stack", j.getTechStack());
        m.put("status", j.getStatus());
        m.put("remark", j.getRemark());
        return m;
    }
}
