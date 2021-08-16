package com.jeremydyer.nifi.nvidia;

public class ProcessResult {

    private int exitCode;
    private String processOutput;

    public ProcessResult() {
        this.exitCode = 1;
    }

    public ProcessResult(int exitCode, String processOutput) {
        this.exitCode = exitCode;
        this.processOutput = processOutput;
    }

    public int getExitCode() {
        return exitCode;
    }

    public void setExitCode(int exitCode) {
        this.exitCode = exitCode;
    }

    public String getProcessOutput() {
        return processOutput;
    }

    public void setProcessOutput(String processOutput) {
        this.processOutput = processOutput;
    }
}
