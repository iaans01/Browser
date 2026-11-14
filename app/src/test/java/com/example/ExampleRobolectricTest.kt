package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.i18n.stringsFor
import com.example.model.AgentActionType
import com.example.model.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Murshed Agent", appName)
    }

    @Test
    fun `bilingual strings resolve for arabic and english`() {
        val ar = stringsFor(AppLanguage.ARABIC)
        val en = stringsFor(AppLanguage.ENGLISH)
        assertTrue(ar.runAgentBtn.isNotBlank())
        assertTrue(en.runAgentBtn.isNotBlank())
        assertEquals("Murshed Agent", en.appTitle)
        assertEquals("وكيل مرشد", ar.appTitle)
    }

    @Test
    fun `action wire parsing maps click type and read_screen`() {
        assertEquals(AgentActionType.READ_SCREEN, AgentActionType.fromWire("read_screen"))
        assertEquals(AgentActionType.CLICK, AgentActionType.fromWire("click"))
        assertEquals(AgentActionType.TYPE, AgentActionType.fromWire("type"))
        assertEquals(AgentActionType.FINISH, AgentActionType.fromWire("finish"))
    }

    @Test
    fun `home dashboard starts productive with quick search bar ready`() {
        val state = com.example.model.AgentUiState()
        assertTrue(state.isHomeDashboardVisible)
        assertEquals("", state.urlInput)
        assertTrue(com.example.browser.SandboxPages.quickLinks.any { it.id == "duckduckgo" })
        assertTrue(com.example.browser.SandboxPages.presets.isNotEmpty())
    }

    @Test
    fun `hub viewmodel dev tools and store order flow work`() {
        val hubVm = com.example.viewmodel.MurshedHubViewModel()
        hubVm.onJsonInputChanged("{\"a\":1,\"b\":[2,3]}")
        hubVm.formatJson()
        assertTrue(hubVm.uiState.value.jsonOutput.contains("\n"))
        assertEquals(null, hubVm.uiState.value.jsonError)

        hubVm.onCodecInputChanged("Murshed")
        hubVm.encodeBase64()
        val encoded = hubVm.uiState.value.codecOutput
        assertTrue(encoded.isNotBlank())
        hubVm.onCodecInputChanged(encoded)
        hubVm.decodeBase64()
        assertEquals("Murshed", hubVm.uiState.value.codecOutput)

        hubVm.setPasswordLength(20)
        assertEquals(20, hubVm.uiState.value.generatedPassword.length)

        // Extension Store install, pin, and tracking parameter cleaner tests
        assertTrue(hubVm.extensionCatalog.any { it.id == "ai_summarizer" })
        hubVm.toggleInstallExtension("pomodoro_widget")
        assertTrue("pomodoro_widget" in hubVm.uiState.value.installedExtensionIds)
        assertTrue("pomodoro_widget" in hubVm.uiState.value.pinnedExtensionIds)

        hubVm.togglePinExtension("pomodoro_widget")
        assertTrue("pomodoro_widget" !in hubVm.uiState.value.pinnedExtensionIds)

        val (cleanedUrl, stripped) = hubVm.cleanTrackingParams(
            "https://example.com/article?id=42&utm_source=x&fbclid=abc123&gclid=xyz"
        )
        assertEquals("https://example.com/article?id=42", cleanedUrl)
        assertEquals(listOf("utm_source", "fbclid", "gclid"), stripped)
    }

    @Test
    fun `local lite language engine handles search dom planning offline summary and suggestions`() {
        val local = com.example.ai.LocalLiteLanguageEngine()
        val snapshot = com.example.model.ScreenSnapshot(
            url = "https://duckduckgo.com/",
            title = "DuckDuckGo",
            scrollPercent = 0,
            pageTextPreview = "DuckDuckGo search engine",
            elements = listOf(
                com.example.model.DomElement(
                    agentId = "el_1",
                    tag = "input",
                    role = "combobox",
                    text = "",
                    placeholder = "Search without being tracked",
                    value = "",
                    inputType = "search",
                    href = "",
                    isEnabled = true,
                    isVisible = true,
                    selector = "#searchbox_input"
                )
            ),
            rawDomJson = "{}"
        )

        val decision = local.decideNextStep(
            goal = "ابحث في DuckDuckGo عن «Jetpack Compose» وافتح أول نتيجة",
            stepNumber = 1,
            snapshot = snapshot,
            history = emptyList(),
            language = AppLanguage.ARABIC,
            fallbackReason = "انتهاء حصة Gemini"
        )
        assertEquals(AgentActionType.TYPE, decision.action)
        assertEquals("el_1", decision.targetAgentId)
        assertEquals("Jetpack Compose", decision.inputText)
        assertTrue(decision.rawJson.contains("local-lite-v1"))

        val offlineSummary = local.summarizePageOffline(
            url = "https://example.com/kotlin",
            title = "Kotlin Multiplatform Overview",
            contentText = "Kotlin Multiplatform simplifies sharing business logic across Android and iOS devices. " +
                "Jetpack Compose enables declarative UI development in Kotlin. " +
                "Developers save significant maintenance time using shared repositories.",
            language = AppLanguage.ENGLISH
        )
        assertTrue(offlineSummary.contains("Local Lite Offline Summary"))
        assertTrue(offlineSummary.contains("Kotlin"))

        val suggestions = local.suggestPrompts(
            query = "Android",
            currentUrl = "https://duckduckgo.com/",
            pageTitle = "DuckDuckGo",
            isHomeMode = true,
            language = AppLanguage.ENGLISH
        )
        assertTrue(suggestions.isNotEmpty())
        assertTrue(suggestions.any { it.text.contains("Android") })
    }

    @Test
    fun `gemini engine falls back cleanly to local lite engine on TimeoutCancellationException`() =
        kotlinx.coroutines.runBlocking {
            val engine = com.example.ai.GeminiAgentEngine()
            val snapshot = com.example.model.ScreenSnapshot(
                url = "https://duckduckgo.com/",
                title = "DuckDuckGo",
                scrollPercent = 0,
                pageTextPreview = "DuckDuckGo search engine",
                elements = listOf(
                    com.example.model.DomElement(
                        agentId = "el_1",
                        tag = "input",
                        role = "combobox",
                        text = "",
                        placeholder = "Search without being tracked",
                        value = "",
                        inputType = "search",
                        href = "",
                        isEnabled = true,
                        isVisible = true,
                        selector = "#searchbox_input"
                    )
                ),
                rawDomJson = "{}"
            )

            val result = engine.decideWithCloudAttempt(
                goal = "ابحث عن «Kotlin Compose»",
                stepNumber = 1,
                snapshot = snapshot,
                history = emptyList(),
                language = AppLanguage.ARABIC
            ) {
                kotlinx.coroutines.withTimeout(10L) {
                    kotlinx.coroutines.delay(500L)
                    error("Should have timed out before reaching here")
                }
            }

            assertEquals(com.example.model.AiEngineSource.LOCAL_LITE_FALLBACK, result.engineSource)
            assertEquals(AgentActionType.TYPE, result.decision.action)
            assertEquals("el_1", result.decision.targetAgentId)
            assertTrue(result.decision.rawJson.contains("local-lite-v1"))
            assertTrue(!result.fallbackReason.isNullOrBlank())
        }

    @Test
    fun `command intent resolver distinguishes direct domain navigation, domain plus task, and web search`() {
        val bareDomain = com.example.browser.CommandIntentResolver.resolve("wikipedia.org")
        assertEquals(com.example.model.CommandIntentKind.DIRECT_OPEN_URL, bareDomain.kind)
        assertEquals("https://wikipedia.org", bareDomain.normalizedUrl)

        val openPhrase = com.example.browser.CommandIntentResolver.resolve("افتح github.com/trending")
        assertEquals(com.example.model.CommandIntentKind.DIRECT_OPEN_URL, openPhrase.kind)
        assertEquals("https://github.com/trending", openPhrase.normalizedUrl)

        val localhostUrl = com.example.browser.CommandIntentResolver.normalizeAddressBarInput("localhost:3000/api")
        assertEquals("http://localhost:3000/api", localhostUrl)

        val openAndAct = com.example.browser.CommandIntentResolver.resolve("افتح wikipedia.org ولخص المقال")
        assertEquals(com.example.model.CommandIntentKind.OPEN_URL_AND_ACT, openAndAct.kind)
        assertEquals("https://wikipedia.org", openAndAct.normalizedUrl)

        val searchAboutDomain = com.example.browser.CommandIntentResolver.resolve("ابحث عن تقييمات amazon.com")
        assertEquals(com.example.model.CommandIntentKind.SEARCH_WEB, searchAboutDomain.kind)

        val forcedOpen = com.example.browser.CommandIntentResolver.resolve(
            rawCommand = "ابحث عن تقييمات amazon.com",
            override = com.example.model.CommandIntentOverride.FORCE_OPEN_URL
        )
        assertEquals(com.example.model.CommandIntentKind.OPEN_URL_AND_ACT, forcedOpen.kind)

        val forcedSearch = com.example.browser.CommandIntentResolver.resolve(
            rawCommand = "wikipedia.org",
            override = com.example.model.CommandIntentOverride.FORCE_SEARCH
        )
        assertEquals(com.example.model.CommandIntentKind.SEARCH_WEB, forcedSearch.kind)

        val filenameNotDomain = com.example.browser.CommandIntentResolver.resolve("لخص report.pdf")
        assertEquals(com.example.model.CommandIntentKind.GENERAL_GOAL, filenameNotDomain.kind)

        val plainSearchAddress = com.example.browser.CommandIntentResolver.normalizeAddressBarInput("أخبار الذكاء الاصطناعي")
        assertTrue(plainSearchAddress.startsWith("https://duckduckgo.com/?q="))
    }

    @Test
    fun `cloud gemini disabled routes both agent decisions and summaries to local model without fallback error`() =
        kotlinx.coroutines.runBlocking {
            val engine = com.example.ai.GeminiAgentEngine()
            val snapshot = com.example.model.ScreenSnapshot(
                url = "https://kotlinlang.org/docs/home.html",
                title = "Kotlin Docs",
                scrollPercent = 0,
                pageTextPreview = "Kotlin Multiplatform and Jetpack Compose official guides.",
                metaDescription = "Official Kotlin language documentation and tutorials.",
                semanticOutline = "H1: Kotlin Docs | MAIN: Getting Started",
                structuredDataSummary = "WebSite: Kotlin Documentation",
                elements = listOf(
                    com.example.model.DomElement(
                        agentId = "el_1",
                        tag = "a",
                        role = "link",
                        text = "Get started",
                        placeholder = "",
                        value = "",
                        inputType = "",
                        href = "https://kotlinlang.org/docs/getting-started.html",
                        isEnabled = true,
                        isVisible = true,
                        selector = "a.start",
                        ariaLabel = "Get started with Kotlin",
                        sectionContext = "main"
                    )
                ),
                rawDomJson = "{}"
            )

            val decisionResult = engine.decideNextStep(
                goal = "اضغط على Get started",
                stepNumber = 1,
                snapshot = snapshot,
                history = emptyList(),
                language = AppLanguage.ARABIC,
                allowCloudGemini = false
            )
            assertEquals(com.example.model.AiEngineSource.LOCAL_LITE_FALLBACK, decisionResult.engineSource)
            assertEquals(null, decisionResult.fallbackReason)
            assertEquals(AgentActionType.CLICK, decisionResult.decision.action)

            val summaryChunks = mutableListOf<com.example.ai.SummaryChunk>()
            engine.streamPageSummary(
                url = snapshot.url,
                title = snapshot.title,
                contentText = snapshot.pageTextPreview,
                language = AppLanguage.ENGLISH,
                allowCloudGemini = false,
                metaDescription = snapshot.metaDescription,
                semanticOutline = snapshot.semanticOutline,
                structuredDataSummary = snapshot.structuredDataSummary
            ).collect { chunk ->
                summaryChunks.add(chunk)
            }
            assertEquals(1, summaryChunks.size)
            val summaryChunk = summaryChunks.first()
            assertEquals(com.example.model.AiEngineSource.LOCAL_LITE_FALLBACK, summaryChunk.engineSource)
            assertEquals(null, summaryChunk.fallbackReason)
            assertTrue(summaryChunk.text.contains("Local Lite Offline Summary"))
            assertTrue(summaryChunk.text.contains("Official Kotlin language documentation"))
        }

    @Test
    fun `compound signup command extracts email separately from target domain and decomposes into steps`() {
        val cmd = "ادخل الى موقع example.com وسجل حساب جديد بالبريد first.last@gmail.com"
        val resolved = com.example.browser.CommandIntentResolver.resolve(cmd)
        assertEquals(com.example.model.CommandIntentKind.OPEN_URL_AND_ACT, resolved.kind)
        assertEquals("example.com", resolved.detectedDomain)
        assertEquals("https://example.com", resolved.normalizedUrl)
        assertEquals("first.last@gmail.com", resolved.entities.email)

        val plan = com.example.browser.CommandIntentResolver.decomposeCommand(cmd)
        assertTrue(plan.steps.any { it.kind == com.example.model.SubStepKind.OPEN_SITE })
        assertTrue(plan.steps.any { it.kind == com.example.model.SubStepKind.OPEN_SIGNUP })
        assertTrue(plan.steps.any { it.kind == com.example.model.SubStepKind.FILL_EMAIL && it.detail == "first.last@gmail.com" })
        assertTrue(plan.steps.any { it.kind == com.example.model.SubStepKind.USER_VERIFICATION })
    }

    @Test
    fun `local lite engine completes bilingual signup steps and pauses with await_user on password`() {
        val local = com.example.ai.LocalLiteLanguageEngine()
        val goal = "ادخل الى موقع example.com وسجل حساب جديد بالبريد user@mail.com"
        val plan = com.example.browser.CommandIntentResolver.decomposeCommand(goal)

        // 1. Landing page with Sign in and Sign up buttons -> must pick Sign up
        val homeSnapshot = com.example.model.ScreenSnapshot(
            url = "https://example.com/",
            title = "Example Portal",
            scrollPercent = 0,
            pageTextPreview = "Welcome to Example Portal",
            elements = listOf(
                com.example.model.DomElement("el_1", "a", "link", "Sign in", "", "", "", "/login", true, true, "a.login"),
                com.example.model.DomElement("el_2", "a", "link", "Sign up", "", "", "", "/signup", true, true, "a.signup")
            ),
            rawDomJson = "{}"
        )
        val step1 = local.decideNextStep(goal, 1, homeSnapshot, emptyList(), AppLanguage.ARABIC, taskPlan = plan)
        assertEquals(AgentActionType.CLICK, step1.action)
        assertEquals("el_2", step1.targetAgentId)

        // 2. Signup form with email field -> must type exact email with submit=false
        val signupFormSnapshot = com.example.model.ScreenSnapshot(
            url = "https://example.com/signup",
            title = "Create account",
            scrollPercent = 0,
            pageTextPreview = "Create your new account",
            elements = listOf(
                com.example.model.DomElement("el_1", "input", "", "", "Work email", "", "email", "", true, true, "#email", autocomplete = "email"),
                com.example.model.DomElement("el_2", "button", "button", "Continue", "", "", "submit", "", true, true, "button.next")
            ),
            rawDomJson = "{}",
            authFormHint = "signup"
        )
        val step2 = local.decideNextStep(goal, 2, signupFormSnapshot, emptyList(), AppLanguage.ARABIC, taskPlan = plan)
        assertEquals(AgentActionType.TYPE, step2.action)
        assertEquals("el_1", step2.targetAgentId)
        assertEquals("user@mail.com", step2.inputText)
        assertEquals(false, step2.submit)

        // 3. After email is filled and password field appears -> must pause with AWAIT_USER
        val passwordSnapshot = com.example.model.ScreenSnapshot(
            url = "https://example.com/signup/password",
            title = "Set password",
            scrollPercent = 0,
            pageTextPreview = "Choose a password",
            elements = listOf(
                com.example.model.DomElement("el_1", "input", "", "", "Email", "user@mail.com", "email", "", true, true, "#email", autocomplete = "email"),
                com.example.model.DomElement("el_2", "input", "", "", "Password", "", "password", "", true, true, "#password", autocomplete = "new-password")
            ),
            rawDomJson = "{}",
            hasPasswordField = true,
            authFormHint = "signup"
        )
        val step3 = local.decideNextStep(goal, 3, passwordSnapshot, emptyList(), AppLanguage.ARABIC, taskPlan = plan)
        assertEquals(AgentActionType.AWAIT_USER, step3.action)
        assertEquals("waiting", step3.phase)
    }

    @Test
    fun `dual login and signup forms on same page bind strictly to matching container and avoid oauth or forgot password`() {
        val local = com.example.ai.LocalLiteLanguageEngine()
        val signupGoal = "ادخل الى موقع example.com وسجل حساب جديد بالبريد newuser@mail.com"
        val signupPlan = com.example.browser.CommandIntentResolver.decomposeCommand(signupGoal)
        assertEquals(com.example.model.AuthIntentKind.SIGN_UP, signupPlan.entities.authIntent)

        // Login form c_1 appears FIRST in DOM order; Sign Up form c_2 appears SECOND
        val dualFormElements = listOf(
            com.example.model.DomElement(
                agentId = "el_1", tag = "button", role = "button", text = "Continue with Google",
                placeholder = "", value = "", inputType = "button", href = "",
                isEnabled = true, isVisible = true, selector = "button.google-oauth",
                containerId = "c_1", containerHeading = "Sign In", authRole = "login"
            ),
            com.example.model.DomElement(
                agentId = "el_2", tag = "input", role = "", text = "",
                placeholder = "Email", value = "", inputType = "email", href = "",
                isEnabled = true, isVisible = true, selector = "#login-email",
                autocomplete = "email", containerId = "c_1", containerHeading = "Sign In", authRole = "login"
            ),
            com.example.model.DomElement(
                agentId = "el_3", tag = "input", role = "", text = "",
                placeholder = "Password", value = "", inputType = "password", href = "",
                isEnabled = true, isVisible = true, selector = "#login-password",
                autocomplete = "current-password", containerId = "c_1", containerHeading = "Sign In", authRole = "login"
            ),
            com.example.model.DomElement(
                agentId = "el_4", tag = "a", role = "link", text = "Forgot password?",
                placeholder = "", value = "", inputType = "", href = "/forgot",
                isEnabled = true, isVisible = true, selector = "a.forgot",
                containerId = "c_1", containerHeading = "Sign In", authRole = "login"
            ),
            com.example.model.DomElement(
                agentId = "el_5", tag = "button", role = "button", text = "Sign in",
                placeholder = "", value = "", inputType = "submit", href = "",
                isEnabled = true, isVisible = true, selector = "button.login-submit",
                containerId = "c_1", containerHeading = "Sign In", authRole = "login"
            ),
            com.example.model.DomElement(
                agentId = "el_6", tag = "input", role = "", text = "",
                placeholder = "Work email", value = "", inputType = "email", href = "",
                isEnabled = true, isVisible = true, selector = "#signup-email",
                autocomplete = "email", containerId = "c_2", containerHeading = "Create Account", authRole = "signup"
            ),
            com.example.model.DomElement(
                agentId = "el_7", tag = "button", role = "button", text = "Continue",
                placeholder = "", value = "", inputType = "submit", href = "",
                isEnabled = true, isVisible = true, selector = "button.signup-submit",
                containerId = "c_2", containerHeading = "Create Account", authRole = "signup"
            )
        )

        val dualSnapshot = com.example.model.ScreenSnapshot(
            url = "https://example.com/auth",
            title = "Sign In or Create Account",
            scrollPercent = 0,
            pageTextPreview = "Sign In Create Account",
            elements = dualFormElements,
            rawDomJson = "{}",
            hasPasswordField = true,
            authFormHint = "dual_forms"
        )

        // Step 1 for Sign Up must pick #signup-email (el_6), NOT #login-email (el_2)
        val signupStep1 = local.decideNextStep(signupGoal, 1, dualSnapshot, emptyList(), AppLanguage.ARABIC, taskPlan = signupPlan)
        assertEquals(AgentActionType.TYPE, signupStep1.action)
        assertEquals("el_6", signupStep1.targetAgentId)
        assertEquals("newuser@mail.com", signupStep1.inputText)

        // Step 2 after signup email is filled must click el_7 (Continue in c_2), ignoring Google OAuth and Login's password
        val afterEmailSnapshot = dualSnapshot.copy(
            elements = dualFormElements.map { if (it.agentId == "el_6") it.copy(value = "newuser@mail.com") else it }
        )
        val signupStep2 = local.decideNextStep(signupGoal, 2, afterEmailSnapshot, emptyList(), AppLanguage.ARABIC, taskPlan = signupPlan)
        assertEquals(AgentActionType.CLICK, signupStep2.action)
        assertEquals("el_7", signupStep2.targetAgentId)

        // Explicit Log In goal on the exact same dual page must pick #login-email (el_2), ignoring Google & Sign Up
        val loginGoal = "ادخل الى موقع example.com لتسجيل الدخول بالبريد existing@mail.com"
        val loginPlan = com.example.browser.CommandIntentResolver.decomposeCommand(loginGoal)
        assertEquals(com.example.model.AuthIntentKind.LOG_IN, loginPlan.entities.authIntent)
        assertTrue(loginPlan.steps.any { it.kind == com.example.model.SubStepKind.OPEN_LOGIN })

        val loginStep1 = local.decideNextStep(loginGoal, 1, dualSnapshot, emptyList(), AppLanguage.ARABIC, taskPlan = loginPlan)
        assertEquals(AgentActionType.TYPE, loginStep1.action)
        assertEquals("el_2", loginStep1.targetAgentId)
        assertEquals("existing@mail.com", loginStep1.inputText)

        // After Login email is filled, Login container c_1 has empty password field (el_3) -> must pause with AWAIT_USER (never click Forgot or Google)
        val afterLoginEmailSnapshot = dualSnapshot.copy(
            elements = dualFormElements.map { if (it.agentId == "el_2") it.copy(value = "existing@mail.com") else it }
        )
        val loginStep2 = local.decideNextStep(loginGoal, 2, afterLoginEmailSnapshot, emptyList(), AppLanguage.ARABIC, taskPlan = loginPlan)
        assertEquals(AgentActionType.AWAIT_USER, loginStep2.action)
        assertEquals("waiting", loginStep2.phase)
    }

    @Test
    fun `tabbed auth page automatically switches inactive target tab before filling email`() {
        val local = com.example.ai.LocalLiteLanguageEngine()
        val goal = "سجل حساب جديد في example.com بالبريد tabuser@mail.com"
        val plan = com.example.browser.CommandIntentResolver.decomposeCommand(goal)

        val tabbedSnapshot = com.example.model.ScreenSnapshot(
            url = "https://example.com/account",
            title = "Account Access",
            scrollPercent = 0,
            pageTextPreview = "Sign In Sign Up",
            elements = listOf(
                com.example.model.DomElement(
                    agentId = "el_1", tag = "button", role = "tab", text = "Sign In",
                    placeholder = "", value = "", inputType = "button", href = "",
                    isEnabled = true, isVisible = true, selector = "#tab-login",
                    authRole = "login", ariaSelected = true
                ),
                com.example.model.DomElement(
                    agentId = "el_2", tag = "button", role = "tab", text = "Sign Up",
                    placeholder = "", value = "", inputType = "button", href = "",
                    isEnabled = true, isVisible = true, selector = "#tab-signup",
                    authRole = "signup", ariaSelected = false
                ),
                com.example.model.DomElement(
                    agentId = "el_3", tag = "input", role = "", text = "",
                    placeholder = "Email", value = "", inputType = "email", href = "",
                    isEnabled = true, isVisible = true, selector = "#login-email",
                    autocomplete = "email", containerId = "c_1", containerHeading = "Sign In", authRole = "login"
                )
            ),
            rawDomJson = "{}",
            authFormHint = "tabbed_login_active"
        )

        val step1 = local.decideNextStep(goal, 1, tabbedSnapshot, emptyList(), AppLanguage.ARABIC, taskPlan = plan)
        assertEquals(AgentActionType.CLICK, step1.action)
        assertEquals("el_2", step1.targetAgentId)
    }

    @Test
    fun `woocommerce html form attributes and nonces disambiguate register from login without headings`() {
        val local = com.example.ai.LocalLiteLanguageEngine()
        val goal = "ادخل الى jeelan.freedev.app وسجل جديد بالبريد buyer@jeelan.com"
        val plan = com.example.browser.CommandIntentResolver.decomposeCommand(goal)
        assertEquals(com.example.model.AuthIntentKind.SIGN_UP, plan.entities.authIntent)

        // Both forms share generic Arabic labels without visible container headings; only HTML classes/ids/nonces distinguish them
        val wooElements = listOf(
            com.example.model.DomElement(
                agentId = "el_1",
                tag = "input",
                role = "",
                text = "",
                placeholder = "",
                value = "",
                inputType = "text",
                href = "",
                isEnabled = true,
                isVisible = true,
                selector = "form.woocommerce-form-login #username",
                labelText = "اسم المستخدم أو البريد الإلكتروني",
                nameAttr = "username",
                autocomplete = "username",
                elementId = "username",
                cssClasses = "woocommerce-Input woocommerce-Input--text input-text",
                containerId = "c_1",
                containerHeading = "",
                containerSignature = "form.woocommerce-form.woocommerce-form-login.login | hidden:woocommerce-login-nonce",
                htmlTagSummary = "<input id=\"username\" name=\"username\" class=\"woocommerce-Input input-text\">",
                authRole = "login"
            ),
            com.example.model.DomElement(
                agentId = "el_2",
                tag = "input",
                role = "",
                text = "",
                placeholder = "",
                value = "",
                inputType = "password",
                href = "",
                isEnabled = true,
                isVisible = true,
                selector = "form.woocommerce-form-login #password",
                labelText = "كلمة المرور",
                nameAttr = "password",
                elementId = "password",
                cssClasses = "woocommerce-Input woocommerce-Input--text input-text",
                containerId = "c_1",
                containerSignature = "form.woocommerce-form.woocommerce-form-login.login | hidden:woocommerce-login-nonce",
                authRole = "login"
            ),
            com.example.model.DomElement(
                agentId = "el_3",
                tag = "button",
                role = "button",
                text = "دخول",
                placeholder = "",
                value = "دخول",
                inputType = "submit",
                href = "",
                isEnabled = true,
                isVisible = true,
                selector = "form.woocommerce-form-login button[name=\"login\"]",
                nameAttr = "login",
                cssClasses = "woocommerce-Button woocommerce-button button woocommerce-form-login__submit",
                containerId = "c_1",
                containerSignature = "form.woocommerce-form.woocommerce-form-login.login | hidden:woocommerce-login-nonce",
                authRole = "login"
            ),
            com.example.model.DomElement(
                agentId = "el_4",
                tag = "input",
                role = "",
                text = "",
                placeholder = "",
                value = "",
                inputType = "email",
                href = "",
                isEnabled = true,
                isVisible = true,
                selector = "#reg_email",
                labelText = "البريد الإلكتروني * مطلوبة",
                nameAttr = "email",
                autocomplete = "email",
                isRequired = true,
                elementId = "reg_email",
                cssClasses = "woocommerce-Input woocommerce-Input--text input-text",
                containerId = "c_2",
                containerHeading = "",
                containerSignature = "form.woocommerce-form.woocommerce-form-register.register | hidden:woocommerce-register-nonce | hint:سيتم إرسال رابط لتعيين كلمة مرور جديدة",
                htmlTagSummary = "<input id=\"reg_email\" name=\"email\" class=\"woocommerce-Input woocommerce-Input--text input-text\" type=\"email\">",
                authRole = "signup"
            ),
            com.example.model.DomElement(
                agentId = "el_5",
                tag = "button",
                role = "button",
                text = "تسجيل جديد",
                placeholder = "",
                value = "تسجيل جديد",
                inputType = "submit",
                href = "",
                isEnabled = true,
                isVisible = true,
                selector = "form.woocommerce-form-register button[name=\"register\"]",
                nameAttr = "register",
                cssClasses = "woocommerce-Button woocommerce-button button woocommerce-form-register__submit",
                containerId = "c_2",
                containerSignature = "form.woocommerce-form.woocommerce-form-register.register | hidden:woocommerce-register-nonce",
                htmlTagSummary = "<button name=\"register\" class=\"woocommerce-Button woocommerce-button button woocommerce-form-register__submit\" type=\"submit\">",
                authRole = "signup"
            )
        )

        val snapshot = com.example.model.ScreenSnapshot(
            url = "https://jeelan.freedev.app/my-account/",
            title = "حسابي",
            scrollPercent = 0,
            pageTextPreview = "تسجيل الدخول تسجيل جديد البريد الإلكتروني",
            elements = wooElements,
            rawDomJson = "{}",
            authFormHint = "dual_forms"
        )

        // Step 1: Must pick #reg_email (el_4) inside woocommerce-form-register, never #username (el_1)
        val step1 = local.decideNextStep(goal, 1, snapshot, emptyList(), AppLanguage.ARABIC, taskPlan = plan)
        assertEquals(AgentActionType.TYPE, step1.action)
        assertEquals("el_4", step1.targetAgentId)
        assertEquals("buyer@jeelan.com", step1.inputText)

        // Step 2: After #reg_email is filled, register form has no password field -> click woocommerce-form-register__submit (el_5)
        val afterEmailSnapshot = snapshot.copy(
            elements = wooElements.map { if (it.agentId == "el_4") it.copy(value = "buyer@jeelan.com") else it }
        )
        val step2 = local.decideNextStep(goal, 2, afterEmailSnapshot, emptyList(), AppLanguage.ARABIC, taskPlan = plan)
        assertEquals(AgentActionType.CLICK, step2.action)
        assertEquals("el_5", step2.targetAgentId)

        // Verify GeminiAgentEngine serializes full HTML metadata into the step prompt
        val gemini = com.example.ai.GeminiAgentEngine()
        val promptJson = gemini.buildStepPrompt(goal, 1, snapshot, emptyList(), AppLanguage.ARABIC, taskPlan = plan)
        assertTrue(promptJson.contains("reg_email"))
        assertTrue(promptJson.contains("woocommerce-form-register__submit"))
        assertTrue(promptJson.contains("woocommerce-register-nonce"))
        assertTrue(promptJson.contains("#reg_email"))

        // Verify localized copy selector strings are present
        val arStrings = com.example.i18n.stringsFor(AppLanguage.ARABIC)
        val enStrings = com.example.i18n.stringsFor(AppLanguage.ENGLISH)
        assertEquals("نسخ المحدد", arStrings.copySelectorBtn)
        assertEquals("Copy selector", enStrings.copySelectorBtn)
    }

    @Test
    fun `failure recognition detects site validation alerts, stalled loops, and network errors across both engines`() {
        val local = com.example.ai.LocalLiteLanguageEngine()
        val gemini = com.example.ai.GeminiAgentEngine()

        // 1. Site validation error (e.g. duplicate email alert on WooCommerce)
        val goal = "سجل جديد في example.com بالبريد taken@mail.com"
        val plan = com.example.browser.CommandIntentResolver.decomposeCommand(goal)
        val siteErrorMsg = "خطأ: يوجد حساب مسجل بالفعل بهذا البريد الإلكتروني."
        val siteErrorSnapshot = com.example.model.ScreenSnapshot(
            url = "https://example.com/my-account/",
            title = "حسابي",
            scrollPercent = 0,
            pageTextPreview = siteErrorMsg,
            elements = listOf(
                com.example.model.DomElement(
                    agentId = "el_1", tag = "input", role = "", text = "",
                    placeholder = "", value = "taken@mail.com", inputType = "email",
                    href = "", isEnabled = true, isVisible = true, selector = "#reg_email",
                    elementId = "reg_email", authRole = "signup"
                )
            ),
            rawDomJson = "{}",
            pageErrors = listOf(siteErrorMsg),
            invalidFields = listOf("#reg_email: البريد الإلكتروني مسجل مسبقاً")
        )

        val localFail = local.decideNextStep(goal, 3, siteErrorSnapshot, emptyList(), AppLanguage.ARABIC, taskPlan = plan)
        assertEquals(AgentActionType.FAIL, localFail.action)
        assertTrue(localFail.finalSummary.contains(siteErrorMsg))

        val siteDiagnosis = com.example.ai.FailureDiagnosisAnalyzer.analyzeFailure(
            goal = goal,
            snapshot = siteErrorSnapshot,
            history = emptyList(),
            language = AppLanguage.ARABIC,
            rawErrorMessage = localFail.finalSummary,
            taskPlan = plan
        )
        assertEquals(com.example.model.FailureCategory.SITE_VALIDATION, siteDiagnosis.category)
        assertTrue(siteDiagnosis.causeSummary.contains(siteErrorMsg))
        assertTrue(siteDiagnosis.evidenceDetail.contains("reg_email"))
        assertTrue(siteDiagnosis.suggestedFixes.isNotEmpty())

        // Verify Gemini step prompt carries page_errors and invalid_fields
        val geminiPrompt = gemini.buildStepPrompt(goal, 3, siteErrorSnapshot, emptyList(), AppLanguage.ARABIC, taskPlan = plan)
        assertTrue(geminiPrompt.contains("page_errors"))
        assertTrue(geminiPrompt.contains("invalid_fields"))
        assertTrue(geminiPrompt.contains("#reg_email"))

        // 2. Stalled loop detection (3 identical clicks on same target & url)
        val repeatedStep = com.example.model.AgentStepLog(
            stepNumber = 1,
            phase = com.example.model.AgentPhase.ACTING,
            url = "https://example.com/form",
            decision = com.example.model.AgentActionDecision(
                stepNumber = 1, phase = "act", thought = "", action = AgentActionType.CLICK,
                targetAgentId = "el_9", inputText = "", submit = false, scrollDirection = "down",
                recoveryAnalysis = "", finalSummary = "", rawJson = "{}"
            ),
            executionResult = com.example.model.ActionExecutionResult(true, "Clicked")
        )
        val loopHistory = listOf(repeatedStep, repeatedStep.copy(stepNumber = 2), repeatedStep.copy(stepNumber = 3))
        val stall = com.example.ai.FailureDiagnosisAnalyzer.detectStallLoop(loopHistory)
        assertTrue(stall != null)

        val cleanSnapshot = siteErrorSnapshot.copy(pageErrors = emptyList(), invalidFields = emptyList())
        val loopFail = local.decideNextStep(goal, 4, cleanSnapshot, loopHistory, AppLanguage.ARABIC, taskPlan = plan)
        assertEquals(AgentActionType.FAIL, loopFail.action)

        val loopDiagnosis = com.example.ai.FailureDiagnosisAnalyzer.analyzeFailure(
            goal = goal,
            snapshot = cleanSnapshot,
            history = loopHistory,
            language = AppLanguage.ARABIC,
            rawErrorMessage = loopFail.finalSummary,
            taskPlan = plan
        )
        assertEquals(com.example.model.FailureCategory.LOOP_STALLED, loopDiagnosis.category)

        // 3. Network / HTTP error diagnosis
        val netDiagnosis = com.example.ai.FailureDiagnosisAnalyzer.analyzeFailure(
            goal = "افتح https://blocked.example.com",
            snapshot = cleanSnapshot.copy(networkErrorSummary = "403 Forbidden"),
            history = emptyList(),
            language = AppLanguage.ENGLISH,
            lastWebError = "HTTP 403 Forbidden"
        )
        assertEquals(com.example.model.FailureCategory.NETWORK_HTTP, netDiagnosis.category)
        assertTrue(netDiagnosis.evidenceDetail.contains("403"))

        // 4. Unmatched non-summary goal emits FAIL instead of false FINISH
        val unmatchedDecision = local.decideNextStep(
            goal = "اضغط على زر تصدير التقرير",
            stepNumber = 1,
            snapshot = cleanSnapshot.copy(scrollPercent = 95),
            history = emptyList(),
            language = AppLanguage.ARABIC
        )
        assertEquals(AgentActionType.FAIL, unmatchedDecision.action)
    }
}


