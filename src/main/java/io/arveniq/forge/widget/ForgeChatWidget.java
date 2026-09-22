package io.arveniq.forge.widget;

import io.arveniq.forge.Json;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Server-rendered, dependency-free assistant drawer styled after Forge's dark
 * agent experience. It talks only to the host application's endpoint.
 */
public final class ForgeChatWidget {
  private static final SecureRandom RANDOM = new SecureRandom();
  private final String id, title, agentName, agentSubtitle, contextLabel, endpoint, placeholder, launcherLabel;
  private final int agentsAvailable;
  private final WidgetShortcut shortcut;
  private final Map<String, Object> context;
  private final List<String> quickPrompts;

  private ForgeChatWidget(Builder builder) {
    id = builder.id == null || builder.id.isBlank() ? "forge-widget-" + Long.toUnsignedString(RANDOM.nextLong(), 36) : safeId(builder.id);
    title = nonBlank(builder.title, "PingLead AI"); agentName = nonBlank(builder.agentName, "Web Research Agent");
    agentSubtitle = nonBlank(builder.agentSubtitle, "is ready to help with your work."); contextLabel = nonBlank(builder.contextLabel, "this page");
    endpoint = nonBlank(builder.endpoint, "/api/forge/chat"); placeholder = nonBlank(builder.placeholder, "Message " + agentName + "...");
    launcherLabel = nonBlank(builder.launcherLabel, "Ask " + title);
    agentsAvailable = Math.max(0, builder.agentsAvailable); shortcut = builder.shortcut == null ? WidgetShortcut.meta("K") : builder.shortcut;
    context = Map.copyOf(builder.context); quickPrompts = List.copyOf(builder.quickPrompts);
  }
  public static Builder builder() { return new Builder(); }

  /** Returns an isolated HTML/CSS/JS fragment suitable for a servlet, JSP, Thymeleaf, or Spring MVC view. */
  public String render() {
    Map<String, Object> configuration = new LinkedHashMap<>();
    configuration.put("endpoint", endpoint); configuration.put("context", context); configuration.put("contextLabel", contextLabel); configuration.put("shortcut", shortcut.toJson());
    StringBuilder html = new StringBuilder();
    html.append("<section id=\"").append(escape(id)).append("\" class=\"forge-widget\" aria-label=\"").append(escape(title)).append("\" hidden>");
    html.append("<style>").append(STYLES.replace("$ROOT", "#" + id)).append("</style>");
    html.append("<div class=\"forge-backdrop\" data-forge-close></div><aside class=\"forge-drawer\" role=\"dialog\" aria-modal=\"true\" aria-label=\"").append(escape(title)).append("\">");
    html.append("<header class=\"forge-header\"><div class=\"forge-heading\"><span class=\"forge-mark\" aria-hidden=\"true\">✧</span><div><strong>").append(escape(title)).append("</strong><small>◉ ").append(agentsAvailable).append(" agents available</small></div></div><button class=\"forge-close\" type=\"button\" data-forge-close aria-label=\"Close assistant\">×</button></header>");
    html.append("<div class=\"forge-agent\"><span>").append(escape(agentName)).append("</span><span aria-hidden=\"true\">⌄</span></div>");
    html.append("<div class=\"forge-context\">Using context <strong>").append(escape(contextLabel)).append("</strong></div>");
    html.append("<main class=\"forge-main\"><span class=\"forge-hero-mark\" aria-hidden=\"true\">✧</span><p class=\"forge-eyebrow\">ASK ").append(escape(title).toUpperCase()).append("</p><h2>Work with your ").append(escape(contextLabel)).append(" data</h2><p class=\"forge-subtitle\">").append(escape(agentName)).append(" ").append(escape(agentSubtitle)).append("</p><div class=\"forge-prompts\">");
    for (String prompt : quickPrompts) html.append("<button type=\"button\" data-forge-prompt=\"").append(escape(prompt)).append("\">").append(escape(prompt)).append("</button>");
    html.append("</div><div class=\"forge-transcript\" aria-live=\"polite\"></div></main>");
    html.append("<form class=\"forge-composer\"><input aria-label=\"Message ").append(escape(agentName)).append("\" placeholder=\"").append(escape(placeholder)).append("\" autocomplete=\"off\"/><button type=\"submit\" aria-label=\"Send message\">↗</button></form></aside>");
    html.append("<script type=\"application/json\" data-forge-config>").append(Json.stringify(configuration)).append("</script>");
    html.append("<script>").append(SCRIPT.replace("$ID", Json.stringify(id))).append("</script></section>");
    return html.toString();
  }
  /** A compact host-page trigger matching the screenshot's "Ask" action. Call {@link #render()} once too. */
  public String renderLauncher() {
    return "<button type=\"button\" data-forge-open=\"" + escape(id) + "\" style=\"border:1px solid #314b78;border-radius:8px;background:#1d3b69;color:#b9d3ff;padding:9px 14px;font:600 13px system-ui;cursor:pointer\">✧ " + escape(launcherLabel) + " <kbd style=\"margin-left:8px;color:#92aed9;font-size:11px\">" + escape(shortcut.display()) + "</kbd></button>";
  }
  public String id() { return id; }

  public static final class Builder {
    private String id, title = "PingLead AI", agentName = "Web Research Agent", agentSubtitle = "is ready to help with your work.", contextLabel = "this page", endpoint = "/api/forge/chat", placeholder, launcherLabel;
    private int agentsAvailable = 7; private WidgetShortcut shortcut = WidgetShortcut.meta("K"); private Map<String, Object> context = Map.of();
    private List<String> quickPrompts = List.of("Summarize this customer or lead.", "Draft a thoughtful follow-up.", "Recommend the next best action.");
    public Builder id(String value) { id = value; return this; }
    public Builder title(String value) { title = value; return this; }
    public Builder agentName(String value) { agentName = value; return this; }
    public Builder agentSubtitle(String value) { agentSubtitle = value; return this; }
    public Builder agentsAvailable(int value) { agentsAvailable = value; return this; }
    public Builder contextLabel(String value) { contextLabel = value; return this; }
    /** Context is posted with a turn for UI convenience only; derive authoritative context in the backend. */
    public Builder context(Map<String, Object> value) { context = value == null ? Map.of() : new LinkedHashMap<>(value); return this; }
    public Builder endpoint(String value) { endpoint = value; return this; }
    public Builder shortcut(WidgetShortcut value) { shortcut = value; return this; }
    public Builder shortcut(String value) { shortcut = WidgetShortcut.parse(value); return this; }
    public Builder placeholder(String value) { placeholder = value; return this; }
    public Builder launcherLabel(String value) { launcherLabel = value; return this; }
    public Builder quickPrompts(List<String> value) { quickPrompts = value == null ? List.of() : new ArrayList<>(value); return this; }
    public ForgeChatWidget build() { return new ForgeChatWidget(this); }
  }

  private static String nonBlank(String value, String fallback) { return value == null || value.isBlank() ? fallback : value.trim(); }
  private static String safeId(String value) {
    String normalized = value.trim();
    if (!normalized.matches("[A-Za-z][A-Za-z0-9_-]{0,79}")) throw new IllegalArgumentException("Widget id must start with a letter and contain only letters, digits, _ or -.");
    return normalized;
  }
  private static String escape(String value) { return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;"); }

  private static final String STYLES = """
    $ROOT{position:fixed;inset:0;z-index:2147483000;font-family:Inter,ui-sans-serif,system-ui,-apple-system,BlinkMacSystemFont,\"Segoe UI\",sans-serif;color:#eef2ff;letter-spacing:-.01em}$ROOT[hidden]{display:none}$ROOT *{box-sizing:border-box}$ROOT .forge-backdrop{position:absolute;inset:0;background:rgba(2,6,13,.46)}$ROOT .forge-drawer{position:absolute;top:0;right:0;display:flex;flex-direction:column;width:min(460px,100vw);height:100%;background:#101722;border-left:1px solid #273246;box-shadow:-24px 0 60px rgba(0,0,0,.35)}$ROOT .forge-header{display:flex;align-items:center;justify-content:space-between;padding:15px 16px 12px}$ROOT .forge-heading{display:flex;gap:10px;align-items:center}$ROOT .forge-mark,$ROOT .forge-hero-mark{display:grid;place-items:center;background:#312550;border:1px solid #715ab2;color:#d5c5ff;border-radius:10px;font-size:24px;width:38px;height:38px;line-height:1}$ROOT .forge-heading strong{display:block;font-size:16px;line-height:18px}$ROOT .forge-heading small{display:block;color:#aab8ca;font-size:12px;font-weight:600;margin-top:3px}$ROOT .forge-close{border:0;background:transparent;color:#abb9cc;font-size:29px;line-height:24px;cursor:pointer;padding:3px 0 6px 12px}$ROOT .forge-close:hover{color:white}$ROOT .forge-agent{margin:0 16px;border:1px solid #2c3a4e;background:#111b29;border-radius:8px;padding:12px;display:flex;justify-content:space-between;color:#b8c8de;font-size:13px;font-weight:650}$ROOT .forge-context{margin:22px 16px 0;padding:0 0 12px;border-bottom:1px solid #253145;color:#aebcd0;font-size:12px}$ROOT .forge-context strong{margin-left:7px;color:#edf1f8}$ROOT .forge-main{padding:32px 22px 18px;overflow:auto;flex:1}$ROOT .forge-hero-mark{width:44px;height:44px;font-size:27px;margin:0 0 14px 6px}$ROOT .forge-eyebrow{font-size:11px;font-weight:800;letter-spacing:.11em;color:#aabfea;margin:0 0 8px 6px}$ROOT h2{font-size:18px;line-height:24px;margin:0 6px 8px;font-weight:560}$ROOT .forge-subtitle{font-size:14px;line-height:20px;color:#adbad0;margin:0 6px 16px}$ROOT .forge-prompts{display:grid;grid-template-columns:1fr 1fr;gap:8px 10px;margin:0 6px}$ROOT .forge-prompts button{min-height:55px;text-align:left;padding:11px;border:1px solid #2a374a;border-radius:8px;background:transparent;color:#ebeff7;font:inherit;font-size:13px;line-height:17px;cursor:pointer}$ROOT .forge-prompts button:hover{border-color:#7660b6;background:#171e2e}$ROOT .forge-transcript{display:grid;gap:10px;padding:20px 6px 6px}$ROOT .forge-answer{padding:12px 13px;border-radius:9px;background:#192235;border:1px solid #2c3b52;color:#dfe7f4;font-size:13px;line-height:19px;white-space:pre-wrap}$ROOT .forge-status{color:#9dacc3;font-size:12px;padding:0 6px}$ROOT .forge-composer{display:flex;gap:8px;align-items:center;margin:0 16px 9px;padding:4px 5px 4px 14px;min-height:52px;border:1px solid #3a4658;background:#111925;border-radius:14px}$ROOT .forge-composer input{min-width:0;flex:1;border:0;outline:0;background:transparent;color:#eff4fb;font:inherit;font-size:13px}$ROOT .forge-composer input::placeholder{color:#738399}$ROOT .forge-composer button{width:39px;height:39px;border:0;border-radius:10px;background:#76659f;color:#d9d1ee;font-size:21px;cursor:pointer}$ROOT .forge-composer button:disabled{opacity:.55;cursor:wait}@media(max-width:520px){$ROOT .forge-drawer{width:100%}$ROOT .forge-backdrop{display:none}}
    """;
  private static final String SCRIPT = """
    (function(){const root=document.getElementById($ID);if(!root)return;const config=JSON.parse(root.querySelector('[data-forge-config]').textContent);const input=root.querySelector('input'),form=root.querySelector('form'),send=form.querySelector('button'),transcript=root.querySelector('.forge-transcript');const open=()=>{root.hidden=false;setTimeout(()=>input.focus(),0)},close=()=>{root.hidden=true};root.querySelectorAll('[data-forge-close]').forEach(button=>button.addEventListener('click',close));document.addEventListener('click',event=>{const trigger=event.target instanceof Element?event.target.closest('[data-forge-open]'):null;if(trigger&&trigger.dataset.forgeOpen===root.id)open()});document.addEventListener('keydown',event=>{const key=event.key===' '?'SPACE':String(event.key).toUpperCase();const s=config.shortcut;if(key===s.key&&event.metaKey===s.meta&&event.ctrlKey===s.ctrl&&event.altKey===s.alt&&event.shiftKey===s.shift){event.preventDefault();open()}});root.querySelectorAll('[data-forge-prompt]').forEach(button=>button.addEventListener('click',()=>{input.value=button.dataset.forgePrompt;form.requestSubmit()}));function appendStatus(text){const node=document.createElement('p');node.className='forge-status';node.textContent=text;transcript.append(node);return node}function id(){return globalThis.crypto&&crypto.randomUUID?crypto.randomUUID():Date.now()+'-'+Math.random().toString(36).slice(2)}function renderEvent(raw,answer){if(raw.type==='response.delta')answer.textContent+=(raw.data&&raw.data.delta)||'';if(raw.type==='response.completed'&&raw.data&&raw.data.text)answer.textContent=raw.data.text}async function consume(response,answer){if(!response.body){answer.textContent='The assistant did not return a response.';return}const reader=response.body.getReader(),decoder=new TextDecoder();let buffer='';for(;;){const part=await reader.read();if(part.done)break;buffer+=decoder.decode(part.value,{stream:true});const frames=buffer.split(/\\r?\\n\\r?\\n/);buffer=frames.pop();for(const frame of frames){const data=frame.split(/\\r?\\n/).filter(line=>line.startsWith('data:')).map(line=>line.slice(5).replace(/^ /,'')).join('\\n');if(!data)continue;try{renderEvent(JSON.parse(data),answer)}catch(_){}}}}form.addEventListener('submit',async event=>{event.preventDefault();const message=input.value.trim();if(!message)return;input.value='';send.disabled=true;const status=appendStatus('Working with '+config.contextLabel+'…');const answer=document.createElement('div');answer.className='forge-answer';transcript.append(answer);try{const response=await fetch(config.endpoint,{method:'POST',credentials:'same-origin',headers:{'content-type':'application/json','accept':'text/event-stream'},body:JSON.stringify({message,clientMessageId:id(),context:config.context})});if(!response.ok)throw new Error('Unable to start assistant');await consume(response,answer);if(!answer.textContent)answer.textContent='No answer was returned.'}catch(error){answer.textContent='Unable to reach the assistant. Please try again.'}finally{status.remove();send.disabled=false;input.focus()}});})();
    """;
}
