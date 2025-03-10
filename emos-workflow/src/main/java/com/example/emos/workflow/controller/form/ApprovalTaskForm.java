package com.example.emos.workflow.controller.form;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Data
public class ApprovalTaskForm {

    @NotBlank(message = "taskId不能为空")
    private String taskId;

    @NotBlank(message = "approval不能为空")
    @Pattern(regexp = "^同意$|^不同意$", message = "approval内容不正确")
    private String approval;

}