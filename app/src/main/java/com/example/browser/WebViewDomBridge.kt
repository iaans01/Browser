package com.example.browser

import android.webkit.WebView
import com.example.model.ActionExecutionResult
import com.example.model.DomElement
import com.example.model.ExtractedImage
import com.example.model.ExtractedLink
import com.example.model.ExtractedTable
import com.example.model.PageExtractionSnapshot
import com.example.model.ScreenSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.json.JSONTokener
import kotlin.coroutines.resume

class WebViewDomBridge {
    @Volatile
    private var webViewRef: WebView? = null

    fun attach(webView: WebView) {
        webViewRef = webView
    }

    fun detach(webView: WebView) {
        if (webViewRef === webView) {
            webViewRef = null
        }
    }

    suspend fun loadUrl(url: String) = withContext(Dispatchers.Main) {
        val wv = webViewRef ?: return@withContext
        val trimmed = url.trim()
        if (trimmed == SandboxPages.SANDBOX_URL) {
            wv.loadDataWithBaseURL(
                SandboxPages.SANDBOX_URL,
                SandboxPages.sandboxHtml,
                "text/html",
                "UTF-8",
                SandboxPages.SANDBOX_URL
            )
        } else {
            val normalized = CommandIntentResolver.normalizeAddressBarInput(trimmed)
            wv.loadUrl(normalized)
        }
    }

    suspend fun reload() = withContext(Dispatchers.Main) {
        val wv = webViewRef ?: return@withContext
        if (wv.url == SandboxPages.SANDBOX_URL) {
            loadUrl(SandboxPages.SANDBOX_URL)
        } else {
            wv.reload()
        }
    }

    suspend fun goBack(): Boolean = withContext(Dispatchers.Main) {
        val wv = webViewRef ?: return@withContext false
        if (wv.canGoBack()) {
            wv.goBack()
            true
        } else {
            false
        }
    }

    suspend fun readScreen(): ScreenSnapshot {
        val script = """
            (function() {
              try {
                const isVis = (el) => {
                  let cur = el;
                  while (cur && cur.nodeType === 1) {
                    const cs = window.getComputedStyle(cur);
                    if (!cs || cs.display === 'none' || cs.visibility === 'hidden' || parseFloat(cs.opacity) === 0) return false;
                    cur = cur.parentElement;
                  }
                  const style = window.getComputedStyle(el);
                  if (style && style.display === 'contents') return true;
                  const rect = el.getBoundingClientRect();
                  return rect.width > 0 && rect.height > 0;
                };
                const classListFor = (el) => {
                  if (el && el.classList && el.classList.length) {
                    return Array.from(el.classList).filter(c => c && !c.startsWith('murshed-')).slice(0, 8);
                  }
                  if (typeof el.className === 'string' && el.className.trim()) {
                    return el.className.trim().split(/\s+/).filter(Boolean).slice(0, 8);
                  }
                  return [];
                };
                const escCss = (s) => (window.CSS && CSS.escape) ? CSS.escape(s) : String(s).replace(/([^a-zA-Z0-9_-])/g, '\\$1');
                const buildNodePart = (el) => {
                  const tag = el.tagName.toLowerCase();
                  if (el.id) return tag + '#' + escCss(el.id);
                  const classes = classListFor(el).slice(0, 5);
                  let part = tag + (classes.length ? '.' + classes.map(escCss).join('.') : '');
                  const nm = el.getAttribute && el.getAttribute('name');
                  if (nm && (tag === 'input' || tag === 'button' || tag === 'select' || tag === 'textarea' || tag === 'form')) {
                    part += '[name="' + nm.replace(/"/g, '\\"') + '"]';
                  }
                  return part;
                };
                const selectorFor = (el) => {
                  try {
                    if (el.id) {
                      const idSel = '#' + escCss(el.id);
                      if (document.querySelectorAll(idSel).length === 1) return idSel;
                    }
                    const leaf = buildNodePart(el);
                    if (document.querySelectorAll(leaf).length === 1) return leaf;
                    const form = el.closest('form');
                    if (form && form !== el) {
                      const formSel = buildNodePart(form);
                      const scoped = formSel + ' ' + leaf;
                      if (document.querySelectorAll(scoped).length === 1) return scoped;
                    }
                    const parts = [];
                    let cur = el;
                    for (let depth = 0; depth < 4 && cur && cur.nodeType === 1 && cur !== document.body; depth++) {
                      let part = buildNodePart(cur);
                      if (!cur.id && cur.parentElement) {
                        const sameTagSibs = Array.from(cur.parentElement.children).filter(c => c.tagName === cur.tagName);
                        if (sameTagSibs.length > 1) {
                          part += ':nth-of-type(' + (sameTagSibs.indexOf(cur) + 1) + ')';
                        }
                      }
                      parts.unshift(part);
                      const joined = parts.join(' > ');
                      if (document.querySelectorAll(joined).length === 1) return joined;
                      if (cur.id) break;
                      cur = cur.parentElement;
                    }
                    return parts.join(' > ') || leaf;
                  } catch (_) {
                    return el.tagName.toLowerCase();
                  }
                };
                const labelFor = (el) => {
                  const id = el.id;
                  if (id && window.CSS && CSS.escape) {
                    const lbl = document.querySelector('label[for="' + CSS.escape(id) + '"]');
                    if (lbl) return (lbl.innerText || lbl.textContent || '').replace(/\s+/g, ' ').trim().slice(0, 80);
                  }
                  const parentLbl = el.closest('label');
                  if (parentLbl) return (parentLbl.innerText || parentLbl.textContent || '').replace(/\s+/g, ' ').trim().slice(0, 80);
                  const labelledBy = el.getAttribute('aria-labelledby');
                  if (labelledBy) {
                    const ref = document.getElementById(labelledBy.split(/\s+/)[0]);
                    if (ref) return (ref.innerText || ref.textContent || '').replace(/\s+/g, ' ').trim().slice(0, 80);
                  }
                  const prev = el.previousElementSibling;
                  if (prev && (prev.tagName.toLowerCase() === 'label' || prev.tagName.toLowerCase() === 'span')) {
                    return (prev.innerText || prev.textContent || '').replace(/\s+/g, ' ').trim().slice(0, 80);
                  }
                  return '';
                };
                const sectionFor = (el) => {
                  const sec = el.closest('main, article, form, nav, header, footer, aside, section, [role="main"], [role="dialog"]');
                  if (!sec) return '';
                  const t = sec.tagName.toLowerCase();
                  return sec.id ? (t + '#' + sec.id.slice(0, 24)) : t;
                };

                // Arabic & BEM/CSS-safe token checks (avoids JS \b failures on Arabic letters & __submit / reg_email)
                const hasLoginWords = (s) => {
                  if (!s) return false;
                  const low = String(s).toLowerCase();
                  const enMatch = /(?:^|[^a-z])(sign[\s_-]*in|signin|log[\s_-]*in|login|user[\s_-]*login|already have an account|remember[\s_-]*me)(?:$|[^a-z])/.test(low) ||
                    /(?:woocommerce-form-login|login-nonce|login__submit|lost_password)/.test(low);
                  const arMatch = /(تسجيل الدخول|تسجيل دخول|سجل الدخول|لديك حساب|دخول لحسابك|تذكرني)/.test(s);
                  return enMatch || arMatch;
                };
                const hasSignupWords = (s) => {
                  if (!s) return false;
                  const low = String(s).toLowerCase();
                  const enMatch = /(?:^|[^a-z])(sign[\s_-]*up|signup|register|registration|reg[\s_-]*email|reg[\s_-]*password|reg[\s_-]*username|create[\s_-]*(?:an[\s_-]*)?account|new[\s_-]*account|join[\s_-]*now|get[\s_-]*started)(?:$|[^a-z])/.test(low) ||
                    /(?:woocommerce-form-register|register-nonce|register__submit|form-register)/.test(low);
                  const arMatch = /(تسجيل جديد|انشاء حساب|إنشاء حساب|حساب جديد|عضوية جديدة|مستخدم جديد|تعيين كلمة مرور جديدة)/.test(s);
                  return enMatch || arMatch;
                };
                const isOAuthOrForgotText = (s) => /(google|apple|facebook|github|microsoft|sso|forgot|reset[\s_-]*password|lost[\s_-]*password|جوجل|غوغل|أبل|فيسبوك|نسيت كلمة|استعادة كلمة)/i.test(s || '');

                const findAuthContainer = (el) => {
                  const directForm = el.closest('form');
                  if (directForm) return directForm;
                  const tabPanel = el.closest('[role="tabpanel"], [role="dialog"], fieldset');
                  if (tabPanel && tabPanel.querySelectorAll('form').length <= 1) return tabPanel;
                  let cur = el.parentElement;
                  for (let d = 0; d < 6 && cur && cur !== document.body; d++) {
                    if (cur.querySelectorAll('form').length > 1) break;
                    const clsId = ((cur.id || '') + ' ' + (typeof cur.className === 'string' ? cur.className : '') + ' ' + (cur.getAttribute('data-testid') || '')).toLowerCase();
                    const hasHead = !!cur.querySelector('h1, h2, h3, h4, legend');
                    const looksLikeCard = /(auth|login|signin|sign-in|signup|sign-up|register|create-account|card|panel|box|modal|tab-pane|u-column)/.test(clsId);
                    if (hasHead || looksLikeCard) {
                      const btns = Array.from(cur.querySelectorAll('button, input[type="submit"], [role="button"]'))
                        .map(b => (b.innerText || b.value || b.getAttribute('aria-label') || b.name || (typeof b.className === 'string' ? b.className : '')).trim())
                        .filter(t => t && !isOAuthOrForgotText(t));
                      const boxLogin = btns.some(hasLoginWords);
                      const boxSignup = btns.some(hasSignupWords);
                      if (!(boxLogin && boxSignup)) return cur;
                    }
                    cur = cur.parentElement;
                  }
                  return el.closest('main, section, article, [role="main"]') || null;
                };

                const containerMap = new WeakMap();
                const containerMetaMap = new Map();
                let containerCounter = 1;

                const inspectContainer = (container) => {
                  if (!container) return { id: '', heading: '', role: '', signature: '' };
                  if (containerMap.has(container)) {
                    const cid = containerMap.get(container);
                    return containerMetaMap.get(cid);
                  }
                  const cid = 'c_' + containerCounter++;
                  containerMap.set(container, cid);

                  let headingEl = container.querySelector('h1, h2, h3, h4, legend, [class*="title"], [class*="heading"]');
                  if (!headingEl) {
                    let sib = container.previousElementSibling;
                    for (let k = 0; k < 2 && sib; k++) {
                      const st = sib.tagName.toLowerCase();
                      if (/^h[1-4]$/.test(st)) { headingEl = sib; break; }
                      const nested = sib.querySelector && sib.querySelector('h1, h2, h3, h4');
                      if (nested) { headingEl = nested; break; }
                      sib = sib.previousElementSibling;
                    }
                  }
                  const labelledById = container.getAttribute('aria-labelledby');
                  const labelledByEl = labelledById ? document.getElementById(labelledById.split(/\s+/)[0]) : null;
                  const headingText = (
                    (headingEl ? (headingEl.innerText || headingEl.textContent || '') : '') ||
                    (labelledByEl ? (labelledByEl.innerText || labelledByEl.textContent || '') : '') ||
                    container.getAttribute('aria-label') ||
                    ''
                  ).replace(/\s+/g, ' ').trim().slice(0, 90);

                  const cClasses = classListFor(container);
                  const cAction = (container.getAttribute('action') || '').trim().slice(0, 80);
                  const cName = (container.getAttribute('name') || '').trim().slice(0, 40);
                  const attrMeta = [
                    container.id || '',
                    cClasses.join(' '),
                    cAction,
                    container.getAttribute('data-testid') || '',
                    container.getAttribute('data-action') || '',
                    cName,
                    container.getAttribute('aria-label') || ''
                  ].join(' ');

                  const hiddenCues = [];
                  for (const h of Array.from(container.querySelectorAll('input[type="hidden"]'))) {
                    const hn = (h.getAttribute('name') || h.id || '').trim();
                    if (!hn) continue;
                    if (/^(wc_order_attribution|utm_|_wp_http_referer)/i.test(hn)) continue;
                    hiddenCues.push(hn.slice(0, 45));
                    if (hiddenCues.length >= 4) break;
                  }
                  const hiddenMeta = hiddenCues.join(' ');

                  let signupScore = 0;
                  let loginScore = 0;
                  if (hasSignupWords(headingText)) signupScore += 6;
                  if (hasLoginWords(headingText)) loginScore += 6;
                  if (hasSignupWords(attrMeta)) signupScore += 6;
                  if (hasLoginWords(attrMeta)) loginScore += 6;
                  if (hasSignupWords(hiddenMeta)) signupScore += 7;
                  if (hasLoginWords(hiddenMeta)) loginScore += 7;

                  if (container.querySelector('input[autocomplete="new-password"]')) signupScore += 6;
                  if (container.querySelector('input[autocomplete="current-password"]')) loginScore += 6;

                  const inputs = Array.from(container.querySelectorAll('input:not([type="hidden"]), textarea'));
                  for (const inp of inputs) {
                    const imeta = ((inp.getAttribute('name') || '') + ' ' + (inp.getAttribute('placeholder') || '') + ' ' + (inp.getAttribute('aria-label') || '') + ' ' + (inp.id || '') + ' ' + classListFor(inp).join(' ')).toLowerCase();
                    if (/(confirm.?pass|repeat.?pass|retype.?pass|تأكيد كلمة|full.?name|first.?name|last.?name|الاسم الكامل|reg_email|reg_password)/.test(imeta)) {
                      signupScore += 5;
                    }
                    if (/(user_login|login_email|login_password)/.test(imeta)) {
                      loginScore += 5;
                    }
                  }

                  const helperText = Array.from(container.querySelectorAll('p, small, span'))
                    .slice(0, 6)
                    .map(p => (p.innerText || '').trim())
                    .join(' ');
                  if (hasSignupWords(helperText)) signupScore += 4;
                  if (hasLoginWords(helperText)) loginScore += 4;

                  const actionBtns = Array.from(container.querySelectorAll('button, input[type="submit"], [role="button"]'));
                  for (const b of actionBtns) {
                    const bt = [
                      b.innerText || '',
                      b.value || '',
                      b.getAttribute('aria-label') || '',
                      b.id || '',
                      b.name || '',
                      classListFor(b).join(' ')
                    ].join(' ').replace(/\s+/g, ' ').trim();
                    if (!bt || isOAuthOrForgotText(bt)) continue;
                    if (hasSignupWords(bt)) signupScore += 6;
                    if (hasLoginWords(bt)) loginScore += 6;
                  }

                  let authRole = '';
                  if (signupScore > loginScore && signupScore >= 4) authRole = 'signup';
                  else if (loginScore > signupScore && loginScore >= 4) authRole = 'login';

                  const tagSig = container.tagName.toLowerCase() +
                    (container.id ? ('#' + container.id.slice(0, 30)) : '') +
                    (cClasses.length ? ('.' + cClasses.slice(0, 4).join('.')) : '');
                  const sigParts = [tagSig];
                  if (cAction) sigParts.push('action=' + cAction.slice(0, 40));
                  if (hiddenCues.length) sigParts.push('hidden:' + hiddenCues.join(','));
                  const signature = sigParts.join(' | ').slice(0, 180);

                  const info = { id: cid, heading: headingText, role: authRole, signature: signature };
                  containerMetaMap.set(cid, info);
                  return info;
                };

                const readTabSelectedState = (el, role, tag) => {
                  const ariaSel = el.getAttribute('aria-selected');
                  if (ariaSel === 'true') return true;
                  if (ariaSel === 'false') return false;
                  const cls = (typeof el.className === 'string' ? el.className : '').toLowerCase();
                  const isTabLike = role === 'tab' || !!el.closest('[role="tablist"]') || el.hasAttribute('data-tab') || /\btab\b/.test(cls);
                  if (!isTabLike) return null;
                  if (/\b(active|selected|current|is-active)\b/.test(cls)) return true;
                  const parentList = el.closest('[role="tablist"]') || el.parentElement;
                  if (parentList) {
                    const sibs = Array.from(parentList.children);
                    const anySibActive = sibs.some(s => s !== el && /\b(active|selected|current|is-active)\b/.test((typeof s.className === 'string' ? s.className : '').toLowerCase()));
                    if (anySibActive) return false;
                  }
                  return null;
                };

                const nodes = Array.from(document.querySelectorAll(
                  'a, button, input, textarea, select, [role="button"], [role="link"], [role="tab"], [role="combobox"], [role="checkbox"], [role="switch"], [contenteditable="true"], [onclick], h1, h2, h3'
                ));
                const elements = [];
                let idx = 1;
                let hasPasswordField = false;
                let hasOtpField = false;
                for (const el of nodes) {
                  if (!isVis(el)) continue;
                  const tag = el.tagName.toLowerCase();
                  const elId = (el.id || '').trim().slice(0, 60);
                  const elClasses = classListFor(el).join(' ').slice(0, 140);
                  const imgAlt = el.querySelector && el.querySelector('img') ? (el.querySelector('img').getAttribute('alt') || '') : '';
                  const ariaLabel = (el.getAttribute('aria-label') || '').replace(/\s+/g, ' ').trim().slice(0, 90);
                  const lblText = (tag === 'input' || tag === 'textarea' || tag === 'select') ? labelFor(el) : '';
                  const nameAttr = (el.getAttribute('name') || '').trim().slice(0, 60);
                  const autocomplete = (el.getAttribute('autocomplete') || '').trim().toLowerCase().slice(0, 40);
                  const text = (el.innerText || el.textContent || ariaLabel || el.getAttribute('title') || imgAlt || lblText || nameAttr || '').replace(/\s+/g, ' ').trim().slice(0, 120);
                  const placeholder = (el.getAttribute('placeholder') || '').trim().slice(0, 80);
                  const val = (typeof el.value === 'string') ? el.value.slice(0, 80) : '';
                  const inputType = (el.getAttribute('type') || '').toLowerCase();
                  const href = (el.getAttribute('href') || '').slice(0, 160);
                  const role = (el.getAttribute('role') || '').toLowerCase();
                  const enabled = !(el.disabled || el.getAttribute('aria-disabled') === 'true');
                  const isRequired = !!(el.required || el.getAttribute('aria-required') === 'true');
                  const isChecked = !!(el.checked || el.getAttribute('aria-checked') === 'true');

                  if (tag === 'input' && (inputType === 'password' || autocomplete === 'new-password' || autocomplete === 'current-password')) {
                    hasPasswordField = true;
                  }
                  const otpMeta = (placeholder + ' ' + lblText + ' ' + ariaLabel + ' ' + nameAttr + ' ' + autocomplete + ' ' + elId + ' ' + elClasses).toLowerCase();
                  if (tag === 'input' && (autocomplete === 'one-time-code' || /(otp|verification.?code|confirm.?code|رمز التحقق|كود التفعيل|2fa)/.test(otpMeta))) {
                    hasOtpField = true;
                  }

                  if (!text && !placeholder && !val && tag !== 'input' && tag !== 'textarea' && tag !== 'select') continue;

                  const ariaSelected = readTabSelectedState(el, role, tag);
                  const containerInfo = (tag === 'h1' || tag === 'h2' || tag === 'h3')
                    ? { id: '', heading: '', role: '', signature: '' }
                    : inspectContainer(findAuthContainer(el));

                  let elementAuthRole = containerInfo.role;
                  const ownMeta = (text + ' ' + ariaLabel + ' ' + nameAttr + ' ' + (el.getAttribute('aria-controls') || '') + ' ' + href + ' ' + elId + ' ' + elClasses).trim();
                  if (role === 'tab' || ariaSelected !== null || !!el.closest('[role="tablist"]')) {
                    if (hasSignupWords(ownMeta)) elementAuthRole = 'signup';
                    else if (hasLoginWords(ownMeta)) elementAuthRole = 'login';
                  } else if (!elementAuthRole && (tag === 'button' || tag === 'a' || tag === 'input' || role === 'button' || role === 'link')) {
                    if (!isOAuthOrForgotText(ownMeta)) {
                      if (hasSignupWords(ownMeta)) elementAuthRole = 'signup';
                      else if (hasLoginWords(ownMeta)) elementAuthRole = 'login';
                    }
                  }

                  const attrBits = ['<' + tag];
                  if (inputType) attrBits.push('type="' + inputType + '"');
                  if (elId) attrBits.push('id="' + elId + '"');
                  if (nameAttr) attrBits.push('name="' + nameAttr + '"');
                  if (autocomplete) attrBits.push('autocomplete="' + autocomplete + '"');
                  if (elClasses) attrBits.push('class="' + elClasses.slice(0, 70) + '"');
                  if (isRequired) attrBits.push('required');
                  const htmlTagSummary = (attrBits.join(' ') + '>').slice(0, 180);

                  const agentId = 'el_' + idx++;
                  el.setAttribute('data-murshed-id', agentId);
                  elements.push({
                    agent_id: agentId,
                    tag: tag,
                    role: role,
                    text: text,
                    placeholder: placeholder,
                    value: val,
                    input_type: inputType,
                    href: href,
                    enabled: enabled,
                    visible: true,
                    selector: selectorFor(el),
                    aria_label: ariaLabel,
                    label_text: lblText,
                    section: sectionFor(el),
                    name_attr: nameAttr,
                    autocomplete: autocomplete,
                    required: isRequired,
                    checked: isChecked,
                    container_id: containerInfo.id,
                    container_heading: containerInfo.heading,
                    auth_role: elementAuthRole,
                    aria_selected: ariaSelected,
                    element_id: elId,
                    css_classes: elClasses,
                    container_signature: containerInfo.signature,
                    html_tag_summary: htmlTagSummary
                  });
                  if (elements.length >= 85) break;
                }

                const captchaEls = document.querySelectorAll('iframe[src*="recaptcha"], iframe[src*="hcaptcha"], iframe[src*="challenges.cloudflare.com"], .g-recaptcha, .h-captcha, .cf-turnstile, [data-sitekey]');
                const bodyLow = ((document.body ? document.body.innerText : '') || '').toLowerCase();
                const hasCaptcha = Array.from(captchaEls).some(isVis) || /(verify you are human|i am not a robot|i'm not a robot|أنا لست روبوت|تحقق أنك إنسان)/.test(bodyLow);

                const distinctContainerRoles = new Set();
                for (const elItem of elements) {
                  if (elItem.container_id && elItem.auth_role) {
                    distinctContainerRoles.add(elItem.auth_role);
                  }
                }
                const hasInactiveSignupTab = elements.some(e => e.aria_selected === false && e.auth_role === 'signup');
                const hasInactiveLoginTab = elements.some(e => e.aria_selected === false && e.auth_role === 'login');
                const hasActiveSignupTab = elements.some(e => e.aria_selected === true && e.auth_role === 'signup');
                const hasActiveLoginTab = elements.some(e => e.aria_selected === true && e.auth_role === 'login');

                const hasSignupCue = hasSignupWords(bodyLow);
                const hasLoginCue = hasLoginWords(bodyLow);
                let authFormHint = '';
                if (hasActiveLoginTab && hasInactiveSignupTab) authFormHint = 'tabbed_login_active';
                else if (hasActiveSignupTab && hasInactiveLoginTab) authFormHint = 'tabbed_signup_active';
                else if (distinctContainerRoles.has('signup') && distinctContainerRoles.has('login')) authFormHint = 'dual_forms';
                else if (distinctContainerRoles.has('signup')) authFormHint = 'signup';
                else if (distinctContainerRoles.has('login')) authFormHint = 'login';
                else if (hasSignupCue && !hasLoginCue) authFormHint = 'signup';
                else if (hasLoginCue && !hasSignupCue) authFormHint = 'login';
                else if (hasSignupCue && hasLoginCue) authFormHint = 'mixed';

                const pageLang = (document.documentElement?.getAttribute('lang') || document.querySelector('meta[http-equiv="content-language"]')?.getAttribute('content') || '').trim().slice(0, 32);
                const metaDesc = (
                  document.querySelector('meta[name="description"]')?.getAttribute('content') ||
                  document.querySelector('meta[property="og:description"]')?.getAttribute('content') ||
                  document.querySelector('meta[name="twitter:description"]')?.getAttribute('content') ||
                  ''
                ).replace(/\s+/g, ' ').trim().slice(0, 400);
                const canonicalUrl = (document.querySelector('link[rel="canonical"]')?.getAttribute('href') || '').trim().slice(0, 300);

                // Semantic HTML outline (landmarks, headings, forms, tables)
                const outlineLines = [];
                const outlineNodes = document.querySelectorAll('header, nav, main, article, section, aside, footer, form, h1, h2, h3, h4, table');
                for (const n of Array.from(outlineNodes)) {
                  const tag = n.tagName.toLowerCase();
                  if (tag === 'h1' || tag === 'h2' || tag === 'h3' || tag === 'h4') {
                    const txt = (n.innerText || n.textContent || '').replace(/\s+/g, ' ').trim().slice(0, 120);
                    if (txt) outlineLines.push('<' + tag + '> ' + txt);
                  } else if (tag === 'table') {
                    const rows = n.querySelectorAll('tr').length;
                    const cap = (n.querySelector('caption')?.innerText || '').replace(/\s+/g, ' ').trim().slice(0, 60);
                    outlineLines.push('<table> (' + rows + ' rows' + (cap ? ': ' + cap : '') + ')');
                  } else if (tag === 'form') {
                    const inputs = n.querySelectorAll('input, textarea, select, button').length;
                    outlineLines.push('<form' + (n.id ? '#' + n.id : '') + '> (' + inputs + ' controls)');
                  } else {
                    const aria = (n.getAttribute('aria-label') || '').trim().slice(0, 40);
                    const idStr = n.id ? ('#' + n.id.slice(0, 24)) : '';
                    outlineLines.push('[' + tag + idStr + (aria ? ' "' + aria + '"' : '') + ']');
                  }
                  if (outlineLines.join('\n').length > 3200) break;
                }
                const semanticOutline = outlineLines.join('\n').slice(0, 3500);

                // Structured JSON-LD extraction
                const ldBlocks = [];
                for (const s of Array.from(document.querySelectorAll('script[type="application/ld+json"]'))) {
                  try {
                    const parsed = JSON.parse(s.textContent || '{}');
                    const items = Array.isArray(parsed) ? parsed : [parsed];
                    for (const item of items) {
                      if (!item || typeof item !== 'object') continue;
                      const type = item['@type'] || '';
                      const name = item.name || item.headline || item.title || '';
                      const desc = item.description || '';
                      if (type || name) {
                        ldBlocks.push(type + ': ' + String(name).slice(0, 100) + (desc ? ' — ' + String(desc).slice(0, 140) : ''));
                      }
                    }
                  } catch (_) {}
                  if (ldBlocks.join('\n').length > 1000) break;
                }
                const structuredData = ldBlocks.join('\n').slice(0, 1200);

                // Deep clean readable text prioritizing semantic main/article containers
                const mainContainer = document.querySelector('main, article, [role="main"]') || document.body;
                const textBlocks = [];
                if (mainContainer) {
                  const seen = new Set();
                  for (const b of Array.from(mainContainer.querySelectorAll('h1,h2,h3,h4,p,li,blockquote,td,th,dt,dd,figcaption,pre'))) {
                    if (!isVis(b)) continue;
                    const t = (b.innerText || '').replace(/\s+/g, ' ').trim();
                    if (t.length >= 12 && !seen.has(t)) {
                      seen.add(t);
                      textBlocks.push(t);
                    }
                    if (textBlocks.join(' | ').length > 4400) break;
                  }
                }
                const fallbackBody = (document.body ? (document.body.innerText || '') : '').replace(/\s+/g, ' ').trim();
                const deepText = (textBlocks.length ? textBlocks.join('\n') : fallbackBody).slice(0, 4500);

                // Compact sanitized HTML skeleton for deep reasoning + full outerHTML for HTML viewer
                let htmlSnippet = '';
                try {
                  const cloneTarget = (document.querySelector('main, article, [role="main"]') || document.body);
                  if (cloneTarget) {
                    const clone = cloneTarget.cloneNode(true);
                    clone.querySelectorAll('script, style, noscript, svg, iframe').forEach(el => el.remove());
                    htmlSnippet = (clone.outerHTML || '').replace(/\s+/g, ' ').slice(0, 4000);
                  }
                } catch (_) {}

                const rawHtmlSource = (document.documentElement ? (document.documentElement.outerHTML || '') : '').slice(0, 60000);

                // Extract visible site error banners / inline validation alerts / invalid fields / HTTP error pages
                const pageErrors = [];
                const seenErrors = new Set();
                const errorSelectors = [
                  '.woocommerce-error li',
                  '.woocommerce-error',
                  '[role="alert"]',
                  '[aria-live="assertive"]',
                  '.alert-danger',
                  '.alert-error',
                  '.error-message',
                  '.invalid-feedback',
                  '.field-error',
                  '.form-error',
                  'ul.errors li',
                  '[data-error]'
                ];
                for (const sel of errorSelectors) {
                  let nodes = [];
                  try { nodes = Array.from(document.querySelectorAll(sel)); } catch (_) {}
                  for (const n of nodes) {
                    if (!isVis(n)) continue;
                    const txt = (n.innerText || n.textContent || n.getAttribute('data-error') || '').replace(/\s+/g, ' ').trim();
                    if (txt.length >= 4 && txt.length <= 320 && !seenErrors.has(txt)) {
                      // Avoid parent container duplicate when a child li was already captured
                      let coveredByExisting = false;
                      for (const existing of seenErrors) {
                        if (txt.indexOf(existing) !== -1 && existing.length >= 8) {
                          coveredByExisting = true;
                          break;
                        }
                      }
                      if (!coveredByExisting) {
                        seenErrors.add(txt);
                        pageErrors.push(txt.slice(0, 220));
                      }
                    }
                    if (pageErrors.length >= 6) break;
                  }
                  if (pageErrors.length >= 6) break;
                }

                const invalidFields = [];
                const seenInvalid = new Set();
                for (const el of Array.from(document.querySelectorAll('input, select, textarea'))) {
                  if (!isVis(el) || (el.type || '').toLowerCase() === 'hidden') continue;
                  const ariaInvalid = el.getAttribute('aria-invalid') === 'true';
                  const hasNativeInvalid = !!(el.validity && el.validity.valid === false && el.validationMessage);
                  const rowWrap = el.closest('.woocommerce-invalid, .has-error, .is-invalid, .form-row-error');
                  if (ariaInvalid || hasNativeInvalid || rowWrap) {
                    const idPart = el.id ? ('#' + el.id) : (el.name ? ('[name="' + el.name + '"]') : el.tagName.toLowerCase());
                    const vMsg = (el.validationMessage || el.getAttribute('data-error') || '').replace(/\s+/g, ' ').trim().slice(0, 120);
                    const entry = vMsg ? (idPart + ': ' + vMsg) : idPart;
                    if (!seenInvalid.has(entry)) {
                      seenInvalid.add(entry);
                      invalidFields.push(entry);
                    }
                    if (invalidFields.length >= 6) break;
                  }
                }

                let networkErrorSummary = '';
                const pageTitleLower = (document.title || '').toLowerCase();
                const bodyHeadLower = (document.body ? (document.body.innerText || '') : '').replace(/\s+/g, ' ').trim().slice(0, 500).toLowerCase();
                const httpErrorPatterns = [
                  '403 forbidden',
                  '404 not found',
                  '500 internal server error',
                  '502 bad gateway',
                  '503 service unavailable',
                  'attention required! | cloudflare',
                  'access denied',
                  'err_name_not_resolved',
                  'err_connection_refused',
                  'err_connection_timed_out',
                  'this site can’t be reached',
                  'webpage not available'
                ];
                for (const pat of httpErrorPatterns) {
                  if (pageTitleLower.indexOf(pat) !== -1 || bodyHeadLower.indexOf(pat) !== -1) {
                    networkErrorSummary = (document.title || pat).slice(0, 140);
                    break;
                  }
                }

                const docHeight = Math.max((document.documentElement ? document.documentElement.scrollHeight : 0) - window.innerHeight, 1);
                const scrollPct = Math.min(100, Math.max(0, Math.round((window.scrollY / docHeight) * 100)));

                return JSON.stringify({
                  url: window.location.href || '',
                  title: document.title || '',
                  page_language: pageLang,
                  meta_description: metaDesc,
                  canonical_url: canonicalUrl,
                  scroll_percent: scrollPct,
                  page_text_preview: deepText,
                  semantic_outline: semanticOutline,
                  structured_data: structuredData,
                  html_snippet: htmlSnippet,
                  raw_html_source: rawHtmlSource,
                  has_password_field: hasPasswordField,
                  has_otp_field: hasOtpField,
                  has_captcha: hasCaptcha,
                  auth_form_hint: authFormHint,
                  page_errors: pageErrors,
                  invalid_fields: invalidFields,
                  network_error_summary: networkErrorSummary,
                  elements: elements
                });
              } catch (e) {
                return JSON.stringify({ error: String(e) });
              }
            })();
        """.trimIndent()

        val json = evaluateJsObject(script)
            ?: throw IllegalStateException("WebView did not return a DOM snapshot.")
        if (json.has("error")) {
            throw IllegalStateException("DOM read_screen failed: ${json.optString("error")}")
        }

        val elementsJson = json.optJSONArray("elements")
        val elements = buildList {
            if (elementsJson != null) {
                for (i in 0 until elementsJson.length()) {
                    val obj = elementsJson.optJSONObject(i) ?: continue
                    add(
                        DomElement(
                            agentId = obj.optString("agent_id"),
                            tag = obj.optString("tag"),
                            role = obj.optString("role"),
                            text = obj.optString("text"),
                            placeholder = obj.optString("placeholder"),
                            value = obj.optString("value"),
                            inputType = obj.optString("input_type"),
                            href = obj.optString("href"),
                            isEnabled = obj.optBoolean("enabled", true),
                            isVisible = obj.optBoolean("visible", true),
                            selector = obj.optString("selector"),
                            ariaLabel = obj.optString("aria_label"),
                            labelText = obj.optString("label_text"),
                            sectionContext = obj.optString("section"),
                            nameAttr = obj.optString("name_attr"),
                            autocomplete = obj.optString("autocomplete"),
                            isRequired = obj.optBoolean("required", false),
                            isChecked = obj.optBoolean("checked", false),
                            containerId = obj.optString("container_id"),
                            containerHeading = obj.optString("container_heading"),
                            authRole = obj.optString("auth_role"),
                            ariaSelected = if (obj.has("aria_selected") && !obj.isNull("aria_selected")) {
                                obj.optBoolean("aria_selected")
                            } else {
                                null
                            },
                            elementId = obj.optString("element_id"),
                            cssClasses = obj.optString("css_classes"),
                            containerSignature = obj.optString("container_signature"),
                            htmlTagSummary = obj.optString("html_tag_summary")
                        )
                    )
                }
            }
        }

        // Exclude giant raw_html_source from rawDomJson so JSON tab stays clean & fast
        val compactJsonForDisplay = JSONObject(json.toString()).apply {
            remove("raw_html_source")
        }

        return ScreenSnapshot(
            url = json.optString("url"),
            title = json.optString("title"),
            scrollPercent = json.optInt("scroll_percent", 0),
            pageTextPreview = json.optString("page_text_preview"),
            elements = elements,
            rawDomJson = compactJsonForDisplay.toString(2),
            pageLanguage = json.optString("page_language"),
            metaDescription = json.optString("meta_description"),
            canonicalUrl = json.optString("canonical_url"),
            semanticOutline = json.optString("semantic_outline"),
            structuredDataSummary = json.optString("structured_data"),
            htmlSnippet = json.optString("html_snippet"),
            rawHtmlSource = json.optString("raw_html_source"),
            hasPasswordField = json.optBoolean("has_password_field", false),
            hasOtpField = json.optBoolean("has_otp_field", false),
            hasCaptcha = json.optBoolean("has_captcha", false),
            authFormHint = json.optString("auth_form_hint"),
            pageErrors = json.optJSONArray("page_errors")?.let { arr ->
                (0 until arr.length()).mapNotNull { idx -> arr.optString(idx).takeIf(String::isNotBlank) }
            }.orEmpty(),
            invalidFields = json.optJSONArray("invalid_fields")?.let { arr ->
                (0 until arr.length()).mapNotNull { idx -> arr.optString(idx).takeIf(String::isNotBlank) }
            }.orEmpty(),
            networkErrorSummary = json.optString("network_error_summary")
        )
    }

    suspend fun extractPageResources(): PageExtractionSnapshot {
        val script = """
            (function() {
              try {
                const title = document.title || '';
                const url = window.location.href || '';
                const pageLang = (document.documentElement?.getAttribute('lang') || document.querySelector('meta[http-equiv="content-language"]')?.getAttribute('content') || '').trim().slice(0, 32);
                const metaDesc = (
                  document.querySelector('meta[name="description"]')?.getAttribute('content') ||
                  document.querySelector('meta[property="og:description"]')?.getAttribute('content') ||
                  ''
                ).trim().slice(0, 400);
                const canonicalUrl = (document.querySelector('link[rel="canonical"]')?.getAttribute('href') || '').trim().slice(0, 300);

                const root = document.querySelector('article, main, [role="main"]') || document.body;
                const blocks = root ? Array.from(root.querySelectorAll('h1,h2,h3,h4,p,li,blockquote,figcaption,dt,dd,pre,code')) : [];
                const seenText = new Set();
                const cleanParts = [];
                for (const b of blocks) {
                  const t = (b.innerText || '').replace(/\s+/g, ' ').trim();
                  if (t.length >= 16 && !seenText.has(t)) {
                    seenText.add(t);
                    cleanParts.push(t);
                  }
                  if (cleanParts.join('\n\n').length > 13500) break;
                }
                const fallbackBody = (document.body ? (document.body.innerText || '') : '').replace(/\s+/g, ' ').trim();
                const cleanArticle = (cleanParts.length ? cleanParts.join('\n\n') : fallbackBody).slice(0, 14000);
                const wordCount = cleanArticle ? cleanArticle.split(/\s+/).filter(Boolean).length : 0;

                const outlineLines = [];
                for (const n of Array.from(document.querySelectorAll('header, nav, main, article, section, aside, footer, form, h1, h2, h3, h4, table'))) {
                  const tag = n.tagName.toLowerCase();
                  if (tag === 'h1' || tag === 'h2' || tag === 'h3' || tag === 'h4') {
                    const t = (n.innerText || '').replace(/\s+/g, ' ').trim().slice(0, 120);
                    if (t) outlineLines.push('<' + tag + '> ' + t);
                  } else if (tag === 'form') {
                    outlineLines.push('<form' + (n.id ? '#' + n.id : '') + '>');
                  } else if (tag === 'table') {
                    outlineLines.push('<table> (' + n.querySelectorAll('tr').length + ' rows)');
                  } else {
                    outlineLines.push('[' + tag + (n.id ? '#' + n.id.slice(0, 20) : '') + ']');
                  }
                  if (outlineLines.join('\n').length > 3200) break;
                }

                const ldBlocks = [];
                for (const s of Array.from(document.querySelectorAll('script[type="application/ld+json"]'))) {
                  try {
                    const parsed = JSON.parse(s.textContent || '{}');
                    const items = Array.isArray(parsed) ? parsed : [parsed];
                    for (const item of items) {
                      if (!item || typeof item !== 'object') continue;
                      const type = item['@type'] || '';
                      const name = item.name || item.headline || item.title || '';
                      const desc = item.description || '';
                      if (type || name) ldBlocks.push(type + ': ' + String(name).slice(0, 100) + (desc ? ' — ' + String(desc).slice(0, 140) : ''));
                    }
                  } catch (_) {}
                  if (ldBlocks.join('\n').length > 1000) break;
                }

                const links = [];
                const seenHref = new Set();
                for (const a of Array.from(document.querySelectorAll('a[href]'))) {
                  const href = a.href || a.getAttribute('href') || '';
                  if (!href || href.startsWith('javascript:') || seenHref.has(href)) continue;
                  seenHref.add(href);
                  const txt = (a.innerText || a.getAttribute('aria-label') || a.getAttribute('title') || href).replace(/\s+/g, ' ').trim().slice(0, 120);
                  links.push({ text: txt || href, href: href.slice(0, 400) });
                  if (links.length >= 60) break;
                }

                const images = [];
                const seenImg = new Set();
                for (const img of Array.from(document.querySelectorAll('img[src]'))) {
                  const src = img.currentSrc || img.src || img.getAttribute('src') || '';
                  if (!src || seenImg.has(src)) continue;
                  seenImg.add(src);
                  const alt = (img.getAttribute('alt') || img.getAttribute('title') || '').replace(/\s+/g, ' ').trim().slice(0, 100);
                  images.push({ alt: alt, src: src.slice(0, 400) });
                  if (images.length >= 40) break;
                }

                const headings = [];
                for (const h of Array.from(document.querySelectorAll('h1,h2,h3,h4'))) {
                  const t = (h.innerText || '').replace(/\s+/g, ' ').trim().slice(0, 140);
                  if (t) headings.push({ tag: h.tagName.toLowerCase(), text: t });
                  if (headings.length >= 35) break;
                }

                const tables = [];
                let tIdx = 1;
                for (const tbl of Array.from(document.querySelectorAll('table'))) {
                  const rows = [];
                  for (const tr of Array.from(tbl.querySelectorAll('tr'))) {
                    const cells = Array.from(tr.querySelectorAll('th,td')).map(c => (c.innerText || '').replace(/\s+/g, ' ').trim().slice(0, 90));
                    if (cells.some(Boolean)) rows.push(cells);
                    if (rows.length >= 16) break;
                  }
                  if (rows.length) {
                    const caption = (tbl.querySelector('caption')?.innerText || ('Table #' + tIdx)).trim().slice(0, 80);
                    tables.push({ caption: caption, rows: rows });
                    tIdx++;
                  }
                  if (tables.length >= 8) break;
                }

                const scriptsCount = document.querySelectorAll('script[src]').length;
                const rawHtmlSource = (document.documentElement ? (document.documentElement.outerHTML || '') : '').slice(0, 60000);

                return JSON.stringify({
                  url: url,
                  title: title,
                  page_language: pageLang,
                  canonical_url: canonicalUrl,
                  meta_description: metaDesc,
                  clean_article: cleanArticle,
                  semantic_outline: outlineLines.join('\n').slice(0, 3500),
                  structured_data: ldBlocks.join('\n').slice(0, 1200),
                  raw_html_source: rawHtmlSource,
                  word_count: wordCount,
                  links: links,
                  images: images,
                  headings: headings,
                  tables: tables,
                  scripts_count: scriptsCount
                });
              } catch (e) {
                return JSON.stringify({ error: String(e) });
              }
            })();
        """.trimIndent()

        val json = evaluateJsObject(script)
            ?: throw IllegalStateException("No active page loaded in WebView.")
        if (json.has("error")) {
            throw IllegalStateException(json.optString("error"))
        }

        val linksArr = json.optJSONArray("links")
        val links = buildList {
            if (linksArr != null) {
                for (i in 0 until linksArr.length()) {
                    val obj = linksArr.optJSONObject(i) ?: continue
                    add(ExtractedLink(text = obj.optString("text"), href = obj.optString("href")))
                }
            }
        }

        val imgsArr = json.optJSONArray("images")
        val images = buildList {
            if (imgsArr != null) {
                for (i in 0 until imgsArr.length()) {
                    val obj = imgsArr.optJSONObject(i) ?: continue
                    add(ExtractedImage(alt = obj.optString("alt"), src = obj.optString("src")))
                }
            }
        }

        val headsArr = json.optJSONArray("headings")
        val headings = buildList {
            if (headsArr != null) {
                for (i in 0 until headsArr.length()) {
                    val obj = headsArr.optJSONObject(i) ?: continue
                    add(obj.optString("tag") to obj.optString("text"))
                }
            }
        }

        val tablesArr = json.optJSONArray("tables")
        val tables = buildList {
            if (tablesArr != null) {
                for (i in 0 until tablesArr.length()) {
                    val obj = tablesArr.optJSONObject(i) ?: continue
                    val rowsArr = obj.optJSONArray("rows")
                    val rows = buildList {
                        if (rowsArr != null) {
                            for (r in 0 until rowsArr.length()) {
                                val rowArr = rowsArr.optJSONArray(r) ?: continue
                                add((0 until rowArr.length()).map { c -> rowArr.optString(c) })
                            }
                        }
                    }
                    add(ExtractedTable(caption = obj.optString("caption"), rows = rows))
                }
            }
        }

        return PageExtractionSnapshot(
            url = json.optString("url"),
            title = json.optString("title"),
            metaDescription = json.optString("meta_description"),
            cleanArticleText = json.optString("clean_article"),
            wordCount = json.optInt("word_count", 0),
            links = links,
            images = images,
            headings = headings,
            tables = tables,
            detectedScriptsCount = json.optInt("scripts_count", 0),
            pageLanguage = json.optString("page_language"),
            canonicalUrl = json.optString("canonical_url"),
            semanticOutline = json.optString("semantic_outline"),
            structuredDataSummary = json.optString("structured_data"),
            rawHtmlSource = json.optString("raw_html_source")
        )
    }

    suspend fun highlightElement(agentId: String, fallbackSelector: String = "") {
        if (agentId.isBlank() && fallbackSelector.isBlank()) return
        val quotedId = JSONObject.quote(agentId)
        val quotedSelector = JSONObject.quote(fallbackSelector)
        val script = """
            (function() {
              let el = $quotedId ? document.querySelector('[data-murshed-id=' + $quotedId + ']') : null;
              if (!el && $quotedSelector) {
                try { el = document.querySelector($quotedSelector); } catch (_) {}
              }
              if (!el) return { ok: false };
              el.scrollIntoView({ behavior: 'smooth', block: 'center' });
              const prevOutline = el.style.outline;
              const prevOffset = el.style.outlineOffset;
              el.style.outline = '3px solid #22D3EE';
              el.style.outlineOffset = '3px';
              setTimeout(() => {
                el.style.outline = prevOutline;
                el.style.outlineOffset = prevOffset;
              }, 1100);
              return { ok: true };
            })();
        """.trimIndent()
        evaluateJsObject(script)
    }

    suspend fun clickElement(agentId: String, fallbackSelector: String = ""): ActionExecutionResult {
        if (agentId.isBlank() && fallbackSelector.isBlank()) {
            return ActionExecutionResult(false, "Missing target_agent_id for click action.")
        }
        val quotedId = JSONObject.quote(agentId)
        val quotedSelector = JSONObject.quote(fallbackSelector)
        val script = """
            (function() {
              try {
                let el = $quotedId ? document.querySelector('[data-murshed-id=' + $quotedId + ']') : null;
                if (!el && $quotedSelector) {
                  try { el = document.querySelector($quotedSelector); } catch (_) {}
                }
                if (!el) {
                  return { success: false, message: 'Element ' + $quotedId + ' not found in current DOM.' };
                }
                const style = window.getComputedStyle(el);
                if (style.display === 'none' || style.visibility === 'hidden') {
                  return { success: false, message: 'Element ' + $quotedId + ' is currently hidden.' };
                }
                if (el.disabled || el.getAttribute('aria-disabled') === 'true') {
                  return { success: false, message: 'Element ' + $quotedId + ' is disabled. Fill required fields or satisfy preconditions first.' };
                }
                el.scrollIntoView({ behavior: 'instant', block: 'center' });
                el.focus();
                el.dispatchEvent(new MouseEvent('mousedown', { bubbles: true, cancelable: true }));
                el.dispatchEvent(new MouseEvent('mouseup', { bubbles: true, cancelable: true }));
                el.click();
                return { success: true, message: 'Clicked ' + $quotedId + ' (' + el.tagName.toLowerCase() + ').' };
              } catch (e) {
                return { success: false, message: 'Click error: ' + String(e) };
              }
            })();
        """.trimIndent()

        val res = evaluateJsObject(script)
            ?: return ActionExecutionResult(false, "No response from WebView while clicking $agentId.")
        return ActionExecutionResult(
            success = res.optBoolean("success", false),
            message = res.optString("message", "Unknown click status")
        )
    }

    suspend fun typeText(agentId: String, text: String, submit: Boolean, fallbackSelector: String = ""): ActionExecutionResult {
        if (agentId.isBlank() && fallbackSelector.isBlank()) {
            return ActionExecutionResult(false, "Missing target_agent_id for type action.")
        }
        val quotedId = JSONObject.quote(agentId)
        val quotedSelector = JSONObject.quote(fallbackSelector)
        val quotedText = JSONObject.quote(text)
        val script = """
            (function() {
              try {
                let el = $quotedId ? document.querySelector('[data-murshed-id=' + $quotedId + ']') : null;
                if (!el && $quotedSelector) {
                  try { el = document.querySelector($quotedSelector); } catch (_) {}
                }
                if (!el) {
                  return { success: false, message: 'Input element ' + $quotedId + ' not found in DOM.' };
                }
                if (el.disabled || el.readOnly) {
                  return { success: false, message: 'Input element ' + $quotedId + ' is disabled or read-only.' };
                }
                const tag = el.tagName.toLowerCase();
                if (tag !== 'input' && tag !== 'textarea' && !el.isContentEditable) {
                  return { success: false, message: 'Element ' + $quotedId + ' (' + tag + ') is not a text input.' };
                }
                el.scrollIntoView({ behavior: 'instant', block: 'center' });
                el.focus();
                if (el.isContentEditable) {
                  el.innerText = $quotedText;
                } else {
                  const proto = tag === 'textarea'
                    ? window.HTMLTextAreaElement.prototype
                    : window.HTMLInputElement.prototype;
                  const setter = Object.getOwnPropertyDescriptor(proto, 'value')?.set;
                  if (setter) {
                    setter.call(el, $quotedText);
                  } else {
                    el.value = $quotedText;
                  }
                }
                el.dispatchEvent(new Event('input', { bubbles: true }));
                el.dispatchEvent(new Event('change', { bubbles: true }));
                el.dispatchEvent(new Event('blur', { bubbles: true }));
                if ($submit) {
                  el.dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter', code: 'Enter', keyCode: 13, which: 13, bubbles: true }));
                  el.dispatchEvent(new KeyboardEvent('keypress', { key: 'Enter', code: 'Enter', keyCode: 13, which: 13, bubbles: true }));
                  el.dispatchEvent(new KeyboardEvent('keyup', { key: 'Enter', code: 'Enter', keyCode: 13, which: 13, bubbles: true }));
                  if (el.form) {
                    if (typeof el.form.requestSubmit === 'function') {
                      el.form.requestSubmit();
                    } else {
                      el.form.submit();
                    }
                  }
                }
                return { success: true, message: 'Typed text into ' + $quotedId + ( $submit ? ' and submitted.' : '.' ) };
              } catch (e) {
                return { success: false, message: 'Type error: ' + String(e) };
              }
            })();
        """.trimIndent()

        val res = evaluateJsObject(script)
            ?: return ActionExecutionResult(false, "No response from WebView while typing into $agentId.")
        return ActionExecutionResult(
            success = res.optBoolean("success", false),
            message = res.optString("message", "Unknown type status")
        )
    }

    suspend fun scrollPage(direction: String): ActionExecutionResult {
        val quotedDir = JSONObject.quote(direction.lowercase())
        val script = """
            (function() {
              const dir = $quotedDir;
              const delta = Math.max(Math.round(window.innerHeight * 0.72), 320);
              if (dir === 'up') window.scrollBy(0, -delta);
              else if (dir === 'top') window.scrollTo(0, 0);
              else if (dir === 'bottom') window.scrollTo(0, document.documentElement.scrollHeight);
              else window.scrollBy(0, delta);
              const docHeight = Math.max(document.documentElement.scrollHeight - window.innerHeight, 1);
              const pct = Math.min(100, Math.max(0, Math.round((window.scrollY / docHeight) * 100)));
              return { success: true, message: 'Scrolled ' + dir + ' (viewport now at ' + pct + '%).' };
            })();
        """.trimIndent()

        val res = evaluateJsObject(script)
            ?: return ActionExecutionResult(false, "No response from WebView while scrolling.")
        return ActionExecutionResult(
            success = res.optBoolean("success", false),
            message = res.optString("message", "Scrolled")
        )
    }

    private suspend fun evaluateJsObject(script: String): JSONObject? =
        kotlinx.coroutines.withTimeoutOrNull(8_000L) {
            withContext(Dispatchers.Main) {
                val wv = webViewRef ?: return@withContext null
                suspendCancellableCoroutine { cont ->
                    try {
                        wv.evaluateJavascript(script) { raw ->
                            if (!cont.isActive) return@evaluateJavascript
                            val parsed = unwrapJsJson(raw)
                            cont.resume(parsed)
                        }
                    } catch (_: Throwable) {
                        if (cont.isActive) cont.resume(null)
                    }
                }
            }
        }

    private fun unwrapJsJson(raw: String?): JSONObject? {
        if (raw.isNullOrBlank() || raw == "null" || raw == "undefined") return null
        return try {
            val trimmed = raw.trim()
            if (trimmed.startsWith("{")) {
                JSONObject(trimmed)
            } else {
                val token = JSONTokener(trimmed).nextValue()
                when (token) {
                    is JSONObject -> token
                    is String -> JSONObject(token)
                    else -> null
                }
            }
        } catch (_: Exception) {
            null
        }
    }
}
