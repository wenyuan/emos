package com.example.emos.workflow.controller.form;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

@Data
public class DeleteProcessByIdForm {
    @NotBlank(message = "instanceId不能为空")
    private String instanceId;

    @NotBlank(message = "type不能为空")
    private String type;

    @NotBlank(message = "reason不能为空")
    private String reason;

    private String uuid;

}