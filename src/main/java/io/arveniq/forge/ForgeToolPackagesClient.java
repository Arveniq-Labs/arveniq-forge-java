package io.arveniq.forge;

import java.util.LinkedHashMap;
import java.util.Map;

/** Client for the Forge Tool Package management API, separate from the Developer API. */
public final class ForgeToolPackagesClient extends ForgeServerClient {
  public ForgeToolPackagesClient(ForgeClientOptions options) { super(options); }
  public Map<String, Object> listPackages() { return request("/tool-packages", "GET", null, ForgeRequestOptions.defaults()); }
  public Map<String, Object> getPackage(String packageId) { return request("/tool-packages/" + requiredId(packageId, "packageId"), "GET", null, ForgeRequestOptions.defaults()); }
  public Map<String, Object> createPackage(Map<String, Object> input) { return request("/tool-packages", "POST", input, ForgeRequestOptions.defaults()); }
  public Map<String, Object> updatePackage(String packageId, Map<String, Object> input) { return request("/tool-packages/" + requiredId(packageId, "packageId"), "PATCH", input, ForgeRequestOptions.defaults()); }
  public Map<String, Object> submitPackage(String packageId) { return request("/tool-packages/" + requiredId(packageId, "packageId") + "/submit", "POST", null, ForgeRequestOptions.defaults()); }
  public Map<String, Object> deprecatePackage(String packageId) { return request("/tool-packages/" + requiredId(packageId, "packageId") + "/deprecate", "POST", null, ForgeRequestOptions.defaults()); }
  public Map<String, Object> getVersion(String versionId) { return request("/tool-package-versions/" + requiredId(versionId, "versionId"), "GET", null, ForgeRequestOptions.defaults()); }
  public Map<String, Object> validateVersion(String versionId) { return request("/tool-package-versions/" + requiredId(versionId, "versionId") + "/validate", "POST", null, ForgeRequestOptions.defaults()); }
  public Map<String, Object> listValidationRuns(String versionId) { return request("/tool-package-versions/" + requiredId(versionId, "versionId") + "/validation-runs", "GET", null, ForgeRequestOptions.defaults()); }
  public Map<String, Object> deployVersion(String versionId, DeploymentEnvironment environment, String reason) {
    Map<String, Object> input = new LinkedHashMap<>(); input.put("environment", environment); input.put("reason", reason);
    return request("/tool-package-versions/" + requiredId(versionId, "versionId") + "/deploy", "POST", input, ForgeRequestOptions.defaults());
  }
  public Map<String, Object> listDeployments() { return request("/tool-package-deployments", "GET", null, ForgeRequestOptions.defaults()); }
  public Map<String, Object> approveDeployment(String deploymentId, String reason) { return deploymentDecision(deploymentId, "approve", reason); }
  public Map<String, Object> rejectDeployment(String deploymentId, String reason) { return deploymentDecision(deploymentId, "reject", reason); }
  public Map<String, Object> rollbackDeployment(String deploymentId, String reason) { return deploymentDecision(deploymentId, "rollback", reason); }
  public Map<String, Object> listAvailableHandlers() { return request("/tool-package-handlers/available", "GET", null, ForgeRequestOptions.defaults()); }
  public Map<String, Object> linkHandlerToTool(String handlerId, String toolId) { return request("/tool-package-handlers/" + requiredId(handlerId, "handlerId") + "/link-tool", "POST", Map.of("toolId", toolId), ForgeRequestOptions.defaults()); }
  public Map<String, Object> createToolDefinitionFromHandler(String handlerId) { return request("/tool-package-handlers/" + requiredId(handlerId, "handlerId") + "/create-tool-definition", "POST", null, ForgeRequestOptions.defaults()); }

  public Map<String, Object> createVersion(String packageId, String version, String commitSha, String artifactRef, DeploymentEnvironment environment, ToolPackageManifest manifest) {
    ForgeManifests.assertValid(manifest);
    Map<String, Object> input = new LinkedHashMap<>(); input.put("version", version); input.put("commitSha", commitSha); input.put("artifactRef", artifactRef); input.put("environment", environment == null ? DeploymentEnvironment.DEVELOPMENT : environment); input.put("manifest", manifest);
    return request("/tool-packages/" + requiredId(packageId, "packageId") + "/versions", "POST", input, ForgeRequestOptions.defaults());
  }
  /** Creates a version from a parsed manifest, useful for the dependency-free command-line tool. */
  public Map<String, Object> createVersion(String packageId, Map<String, Object> input) {
    ValidationResult validation = ForgeManifests.validateJson(input == null ? null : input.get("manifest"));
    if (!validation.valid()) throw new IllegalArgumentException("Invalid tool package manifest: " + validation.errors());
    return request("/tool-packages/" + requiredId(packageId, "packageId") + "/versions", "POST", input, ForgeRequestOptions.defaults());
  }
  private Map<String, Object> deploymentDecision(String deploymentId, String action, String reason) {
    Map<String, Object> input = new LinkedHashMap<>(); input.put("reason", reason);
    return request("/tool-package-deployments/" + requiredId(deploymentId, "deploymentId") + "/" + action, "POST", input, ForgeRequestOptions.defaults());
  }
}
