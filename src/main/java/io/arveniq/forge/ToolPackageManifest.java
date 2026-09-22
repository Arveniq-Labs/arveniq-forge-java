package io.arveniq.forge;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Complete, validated definition of a governed Forge tool package. */
public record ToolPackageManifest(
    String name, String namespace, String version, String description,
    ToolPackageRuntimeType runtimeType, String entrypoint, List<ToolPackageHandlerManifest> handlers) implements Json.WritableJson {
  public ToolPackageManifest {
    description = description == null ? "" : description;
    runtimeType = runtimeType == null ? ToolPackageRuntimeType.NODE : runtimeType;
    entrypoint = entrypoint == null ? "src/index.ts" : entrypoint;
    handlers = handlers == null ? List.of() : List.copyOf(handlers);
  }
  @Override public Map<String, Object> toJson() {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("name", name); result.put("namespace", namespace); result.put("version", version); result.put("description", description);
    result.put("runtime", Map.of("type", runtimeType, "entrypoint", entrypoint)); result.put("handlers", handlers.stream().map(ToolPackageHandlerManifest::toJson).toList()); return result;
  }
}
