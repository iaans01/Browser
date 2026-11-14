package com.example.browser

import com.example.model.QuickLinkBookmark
import com.example.model.TaskPreset

object SandboxPages {
    const val DUCKDUCKGO_URL = "https://duckduckgo.com/"
    const val SANDBOX_URL = "https://sandbox.murshed.local/"

    val quickLinks: List<QuickLinkBookmark> = listOf(
        QuickLinkBookmark(
            id = "sandbox_store",
            titleAr = "متجر الإضافات",
            titleEn = "Extension Store",
            subtitleAr = "إضافات متصفح وأدوات جانبية",
            subtitleEn = "Browser side extensions",
            url = SANDBOX_URL,
            isSandbox = true
        ),
        QuickLinkBookmark(
            id = "duckduckgo",
            titleAr = "DuckDuckGo",
            titleEn = "DuckDuckGo",
            subtitleAr = "بحث خاص وسريع",
            subtitleEn = "Private fast search",
            url = DUCKDUCKGO_URL
        ),
        QuickLinkBookmark(
            id = "wiki_ar",
            titleAr = "ويكيبيديا",
            titleEn = "Wikipedia",
            subtitleAr = "الموسوعة الحرة",
            subtitleEn = "Free encyclopedia",
            url = "https://ar.m.wikipedia.org/"
        ),
        QuickLinkBookmark(
            id = "github",
            titleAr = "GitHub",
            titleEn = "GitHub",
            subtitleAr = "مستودعات البرمجة",
            subtitleEn = "Code repositories",
            url = "https://github.com/"
        ),
        QuickLinkBookmark(
            id = "hacker_news",
            titleAr = "Hacker News",
            titleEn = "Hacker News",
            subtitleAr = "أخبار التقنية",
            subtitleEn = "Tech & startup news",
            url = "https://news.ycombinator.com/"
        )
    )

    val presets: List<TaskPreset> = listOf(
        TaskPreset(
            id = "ddg_quick",
            titleAr = "DuckDuckGo: أداة البحث السريع",
            titleEn = "DuckDuckGo: Quick Web Search",
            url = DUCKDUCKGO_URL,
            goalAr = "ابحث في DuckDuckGo عن «Jetpack Compose Android» ثم افتح أول نتيجة مناسبة.",
            goalEn = "Search DuckDuckGo for 'Jetpack Compose Android' and open the first relevant result.",
            isSandbox = false
        ),
        TaskPreset(
            id = "sandbox_budget_bundle",
            titleAr = "متجر التجربة: تجهيز باقة مكتب + شحن سريع وكود PRO30",
            titleEn = "Sandbox Store: Build Desk Bundle + Express & PRO30",
            url = SANDBOX_URL,
            goalAr = "رتب المنتجات من الأقل سعراً، أضف مصباح المكتب وموزع USB-C ولوحة المفاتيح الميكانيكية للسلة، افتح السلة، اختر الشحن السريع، أدخل الاسم «سارة أحمد» والعنوان «الرياض - حي النخيل» وكود الخصم PRO30 ثم أكد الطلب.",
            goalEn = "Sort products by lowest price, add Smart LED Desk Lamp, USB-C Hub, and Mechanical Keyboard to cart, open Cart, select Express Delivery, fill Name 'Sara Ahmed', Address 'Riyadh Al Nakheel', Promo 'PRO30', and submit order.",
            isSandbox = true
        ),
        TaskPreset(
            id = "sandbox_checkout",
            titleAr = "متجر التجربة: شراء سماعة + كود خصم MURSHED20",
            titleEn = "Sandbox Store: Add Headphones + Promo Checkout",
            url = SANDBOX_URL,
            goalAr = "أضف سماعة الرأس اللاسلكية إلى السلة، ثم افتح السلة، اكتب الاسم «سارة أحمد» والعنوان «الرياض - حي النخيل» وكود الخصم MURSHED20 واضغط تأكيد الطلب.",
            goalEn = "Add the Wireless Studio Headphones to the cart, open Checkout, fill Name 'Sara Ahmed', Address 'Riyadh Al Nakheel', Promo code 'MURSHED20', and submit the order.",
            isSandbox = true
        ),
        TaskPreset(
            id = "sandbox_category_wishlist",
            titleAr = "متجر التجربة: تصفية الملحقات وحفظ المفضلة",
            titleEn = "Sandbox Store: Filter Peripherals & Save Wishlist",
            url = SANDBOX_URL,
            goalAr = "اضغط فئة «ملحقات / Peripherals»، احفظ الفأرة المريحة في المفضلة، أضف لوحة المفاتيح للسلة ثم افتح السلة.",
            goalEn = "Filter by Peripherals, save Ergonomic Wireless Mouse to Wishlist, add Mechanical Keyboard to cart, then open Cart.",
            isSandbox = true
        ),
        TaskPreset(
            id = "sandbox_search",
            titleAr = "متجر التجربة: بحث عن منتج ومضاعفة الكمية",
            titleEn = "Sandbox Store: Search USB-C & Set Quantity to 2",
            url = SANDBOX_URL,
            goalAr = "ابحث في المتجر عن «USB-C»، أضف موزع USB-C إلى السلة، ثم افتح السلة وزد كميته إلى 2.",
            goalEn = "Search the store for 'USB-C', add USB-C 8-in-1 Hub to cart, open Cart and increase its quantity to 2.",
            isSandbox = true
        ),
        TaskPreset(
            id = "wiki_ar",
            titleAr = "ويكيبيديا العربية: البحث عن الذكاء الاصطناعي",
            titleEn = "Arabic Wikipedia: Search Artificial Intelligence",
            url = "https://ar.m.wikipedia.org/",
            goalAr = "ابحث عن «الذكاء الاصطناعي» وافتح المقالة الرئيسية.",
            goalEn = "Search for 'الذكاء الاصطناعي' and open the main article.",
            isSandbox = false
        ),
        TaskPreset(
            id = "ddg_lite",
            titleAr = "DuckDuckGo Lite: بحث خفيف وسريع",
            titleEn = "DuckDuckGo Lite: Fast HTML Search",
            url = "https://html.duckduckgo.com/html/",
            goalAr = "ابحث عن Android Kotlin News ثم افتح أول نتيجة مناسبة.",
            goalEn = "Search for 'Android Kotlin News' and open the first relevant result.",
            isSandbox = false
        )
    )

    val sandboxHtml: String = """
        <!DOCTYPE html>
        <html lang="ar" dir="rtl">
        <head>
          <meta charset="UTF-8" />
          <meta name="viewport" content="width=device-width, initial-scale=1.0" />
          <title>Murshed Sandbox Store | متجر مرشد الإنتاجي</title>
          <style>
            :root {
              --bg: #0b111c;
              --card: #151e2e;
              --card-alt: #1b263b;
              --accent: #22d3ee;
              --violet: #a78bfa;
              --green: #34d399;
              --amber: #f59e0b;
              --danger: #fb7185;
              --text: #f8fafc;
              --muted: #94a3b8;
              --border: #2b3a52;
            }
            * { box-sizing: border-box; font-family: system-ui, -apple-system, sans-serif; }
            body { margin: 0; padding: 14px; background: var(--bg); color: var(--text); }
            header { display: flex; justify-content: space-between; align-items: center; gap: 8px; flex-wrap: wrap; margin-bottom: 12px; }
            .brand { font-weight: 800; font-size: 18px; color: var(--accent); }
            .sub-brand { font-size: 12px; color: var(--muted); }
            .header-actions { display: flex; gap: 8px; align-items: center; }
            .cart-btn { background: var(--card); color: var(--text); border: 1px solid var(--accent); border-radius: 10px; padding: 8px 12px; font-weight: 700; cursor: pointer; font-size: 13px; }
            .wish-btn { background: var(--card); color: var(--violet); border: 1px solid var(--violet); border-radius: 10px; padding: 8px 12px; font-weight: 700; cursor: pointer; font-size: 13px; }
            .search-row { display: flex; gap: 8px; margin-bottom: 10px; }
            input { width: 100%; padding: 10px 12px; border-radius: 10px; border: 1px solid var(--border); background: #090e17; color: var(--text); font-size: 14px; }
            button { border: none; border-radius: 10px; padding: 9px 13px; font-weight: 700; cursor: pointer; font-size: 13px; }
            .primary-btn { background: var(--accent); color: #04131c; }
            .green-btn { background: var(--green); color: #032117; }
            .ghost-btn { background: var(--card-alt); color: var(--text); border: 1px solid var(--border); }
            .chip-row { display: flex; gap: 6px; overflow-x: auto; padding-bottom: 6px; margin-bottom: 8px; }
            .chip { background: var(--card); color: var(--muted); border: 1px solid var(--border); border-radius: 99px; padding: 6px 12px; font-size: 12px; white-space: nowrap; }
            .chip.active { background: #0b3b48; color: var(--accent); border-color: var(--accent); }
            .sort-row { display: flex; justify-content: space-between; align-items: center; gap: 6px; flex-wrap: wrap; margin-bottom: 12px; font-size: 12px; color: var(--muted); }
            .sort-btns { display: flex; gap: 6px; }
            .status-bar { background: var(--card); border: 1px solid var(--border); border-radius: 12px; padding: 9px 12px; margin-bottom: 12px; font-size: 12px; color: var(--accent); display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 6px; }
            .grid { display: grid; grid-template-columns: 1fr; gap: 10px; }
            .card { background: var(--card); border: 1px solid var(--border); border-radius: 14px; padding: 13px; }
            .card-top { display: flex; justify-content: space-between; align-items: center; gap: 8px; margin-bottom: 6px; }
            .price { color: var(--green); font-weight: 800; font-size: 16px; }
            .muted { color: var(--muted); font-size: 12px; margin: 4px 0 10px; line-height: 1.45; }
            .badge { display: inline-block; padding: 3px 9px; border-radius: 99px; background: #0b3b48; color: var(--accent); font-size: 11px; font-weight: 700; }
            .card-actions { display: flex; gap: 8px; flex-wrap: wrap; }
            .hidden { display: none; }
            .field { margin-bottom: 10px; }
            label { display: block; font-size: 12px; color: var(--muted); margin-bottom: 4px; }
            button:disabled { opacity: 0.42; cursor: not-allowed; }
            .cart-line { display: flex; justify-content: space-between; align-items: center; gap: 8px; padding: 8px; background: #0d1420; border: 1px solid var(--border); border-radius: 10px; margin-bottom: 6px; font-size: 13px; }
            .qty-controls { display: flex; align-items: center; gap: 6px; }
            .qty-btn { padding: 5px 10px; border-radius: 8px; background: var(--card-alt); color: var(--text); border: 1px solid var(--border); }
            .danger-btn { padding: 5px 9px; border-radius: 8px; background: #3b1320; color: var(--danger); border: 1px solid var(--danger); }
            .ship-row { display: flex; gap: 8px; margin-bottom: 10px; flex-wrap: wrap; }
            .hint-warn { background: #332409; border: 1px solid var(--amber); color: #fde68a; border-radius: 10px; padding: 8px 10px; font-size: 12px; margin-bottom: 10px; }
            .success-box { background: #0d3d2e; border: 1px solid var(--green); padding: 15px; border-radius: 14px; margin-top: 14px; }
            .history-box { background: var(--card); border: 1px solid var(--border); border-radius: 14px; padding: 13px; margin-top: 14px; }
          </style>
        </head>
        <body>
          <header>
            <div>
              <div class="brand">متجر مرشد | Murshed Workspace Store</div>
              <div class="sub-brand">بيئة تسوق ومكتب ذكي لاختبار الوكيل التلقائي</div>
            </div>
            <div class="header-actions">
              <button id="toggle-wishlist-btn" class="wish-btn" onclick="toggleWishlistFilter()">المفضلة / Wishlist (<span id="wish-count">0</span>)</button>
              <button id="open-cart-btn" class="cart-btn" onclick="toggleCart()">السلة / Cart (<span id="cart-count">0</span>)</button>
            </div>
          </header>

          <div class="search-row">
            <input id="search-input" type="text" placeholder="ابحث عن منتج... Search product (e.g. Headphones, USB-C, Mouse, Lamp)" oninput="renderCatalog()" />
            <button id="search-btn" class="primary-btn" onclick="applyFilter()">بحث / Search</button>
            <button id="clear-filter-btn" class="ghost-btn" onclick="resetFilters()">إعادة ضبط / Reset</button>
          </div>

          <div class="chip-row" id="category-chips">
            <button id="cat-all" class="chip active" onclick="setCategory('all')">الكل / All</button>
            <button id="cat-audio" class="chip" onclick="setCategory('Audio')">صوتيات / Audio</button>
            <button id="cat-peripherals" class="chip" onclick="setCategory('Peripherals')">ملحقات / Peripherals</button>
            <button id="cat-desk" class="chip" onclick="setCategory('Desk')">مكتب / Desk</button>
            <button id="cat-accessories" class="chip" onclick="setCategory('Accessories')">إكسسوارات / Accessories</button>
          </div>

          <div class="sort-row">
            <span id="visible-count-label">6 منتجات معروضة</span>
            <div class="sort-btns">
              <button id="sort-default-btn" class="chip active" onclick="setSort('default')">الافتراضي / Default</button>
              <button id="sort-low-btn" class="chip" onclick="setSort('low')">الأقل سعراً / Price Low</button>
              <button id="sort-high-btn" class="chip" onclick="setSort('high')">الأعلى سعراً / Price High</button>
            </div>
          </div>

          <div id="store-status-banner" class="status-bar">
            <span id="status-filter-text">الفئة: الكل | الترتيب: الافتراضي</span>
            <span id="status-subtotal-text">إجمالي السلة: $0</span>
          </div>

          <div id="catalog" class="grid"></div>

          <section id="checkout-panel" class="card hidden" style="margin-top:14px;">
            <h3 style="margin-top:0;">إتمام الطلب / Checkout</h3>
            <div id="cart-items-list" style="margin-bottom:10px;">
              <div class="muted">السلة فارغة / Cart is empty</div>
            </div>

            <div class="field">
              <label>طريقة الشحن / Delivery Speed</label>
              <div class="ship-row">
                <button id="ship-standard-btn" class="chip active" onclick="setShipping('standard')">شحن قياسي مجاني / Standard ($0)</button>
                <button id="ship-express-btn" class="chip" onclick="setShipping('express')">شحن سريع 24 ساعة / Express ($15)</button>
              </div>
            </div>

            <div class="field">
              <label for="customer-name">الاسم الكامل / Full Name</label>
              <input id="customer-name" type="text" placeholder="Enter your full name" oninput="validateForm()" />
            </div>
            <div class="field">
              <label for="customer-address">العنوان / Delivery Address</label>
              <input id="customer-address" type="text" placeholder="City, Street address" oninput="validateForm()" />
            </div>
            <div class="field">
              <label for="promo-code">كود الخصم / Promo Code (MURSHED20 = 20% | PRO30 = 30% over $150)</label>
              <input id="promo-code" type="text" placeholder="MURSHED20 or PRO30" oninput="validateForm()" />
            </div>

            <div id="promo-feedback" class="muted" style="margin-bottom:6px;"></div>
            <div id="total-line" style="font-weight:800; font-size:15px; color:var(--green); margin:8px 0;">الإجمالي / Total: $0</div>
            <div id="form-validation-hint" class="hint-warn">أضف منتجاً واحداً على الأقل واكتب الاسم والعنوان لتفعيل تأكيد الطلب.</div>

            <div class="card-actions">
              <button id="submit-order-btn" class="green-btn" disabled onclick="submitOrder()">تأكيد الطلب / Submit Order</button>
              <button id="close-cart-btn" class="ghost-btn" onclick="toggleCart()">إغلاق / Back</button>
            </div>
          </section>

          <section id="order-confirmation" class="success-box hidden">
            <h3 style="margin-top:0;">تم استلام طلبك بنجاح! / Order Confirmed!</h3>
            <p id="confirmation-details"></p>
            <button id="new-order-btn" class="primary-btn" onclick="resetStore()">طلب جديد / New Order</button>
          </section>

          <section id="order-history-section" class="history-box hidden">
            <h3 style="margin-top:0;">سجل طلبات الجلسة / Session Orders (<span id="orders-count">0</span>)</h3>
            <div id="order-history-list"></div>
          </section>

          <script>
            const PRODUCTS = [
              { id: 'headphones', name: 'سماعة رأس لاسلكية | Wireless Studio Headphones', category: 'Audio', categoryAr: 'صوتيات', price: 149, desc: 'عزل ضوضاء نشط وبطارية 40 ساعة للتركيز.' },
              { id: 'keyboard', name: 'لوحة مفاتيح ميكانيكية | Mechanical Keyboard', category: 'Peripherals', categoryAr: 'ملحقات', price: 95, desc: 'مفاتيح سريعة وإضاءة خلفية هادئة.' },
              { id: 'mouse', name: 'فأرة مريحة لاسلكية | Ergonomic Wireless Mouse', category: 'Peripherals', categoryAr: 'ملحقات', price: 68, desc: 'تصميم مريح لليد ودقة تتبع عالية.' },
              { id: 'stand', name: 'حامل شاشة ألمنيوم | Aluminum Monitor Stand', category: 'Desk', categoryAr: 'مكتب', price: 55, desc: 'يرفع الشاشة لوضعية صحية مع تنظيم الأسلاك.' },
              { id: 'hub', name: 'موزع USB-C متعدد المنافذ | USB-C 8-in-1 Hub', category: 'Accessories', categoryAr: 'إكسسوارات', price: 45, desc: 'يدعم HDMI 4K وشحن سريع حتى 100 واط.' },
              { id: 'lamp', name: 'مصباح مكتب ذكي | Smart LED Desk Lamp', category: 'Desk', categoryAr: 'مكتب', price: 39, desc: 'إضاءة قابلة للتعتيم بدون وهج للقراءة والعمل.' }
            ];

            let cart = [];
            let wishlist = new Set();
            let activeCategory = 'all';
            let activeSort = 'default';
            let onlyWishlist = false;
            let shippingType = 'standard';
            let sessionOrders = [];

            function setCategory(cat) {
              activeCategory = cat;
              onlyWishlist = false;
              ['all','Audio','Peripherals','Desk','Accessories'].forEach(c => {
                const btn = document.getElementById('cat-' + c.toLowerCase());
                if (btn) btn.classList.toggle('active', c === cat);
              });
              renderCatalog();
            }

            function setSort(mode) {
              activeSort = mode;
              ['default','low','high'].forEach(m => {
                const btn = document.getElementById('sort-' + m + '-btn');
                if (btn) btn.classList.toggle('active', m === mode);
              });
              renderCatalog();
            }

            function toggleWishlistFilter() {
              onlyWishlist = !onlyWishlist;
              renderCatalog();
            }

            function resetFilters() {
              document.getElementById('search-input').value = '';
              activeCategory = 'all';
              activeSort = 'default';
              onlyWishlist = false;
              setCategory('all');
              setSort('default');
            }

            function applyFilter() {
              renderCatalog();
            }

            function toggleWishlistItem(id) {
              if (wishlist.has(id)) wishlist.delete(id);
              else wishlist.add(id);
              document.getElementById('wish-count').innerText = wishlist.size;
              renderCatalog();
            }

            function addToCart(id) {
              const p = PRODUCTS.find(x => x.id === id);
              if (!p) return;
              const existing = cart.find(x => x.id === id);
              if (existing) existing.qty += 1;
              else cart.push({ id: p.id, name: p.name, price: p.price, qty: 1 });
              updateCartBadge();
              renderCart();
              validateForm();
            }

            function changeQty(id, delta) {
              const item = cart.find(x => x.id === id);
              if (!item) return;
              item.qty += delta;
              if (item.qty <= 0) {
                cart = cart.filter(x => x.id !== id);
              }
              updateCartBadge();
              renderCart();
              validateForm();
            }

            function removeFromCart(id) {
              cart = cart.filter(x => x.id !== id);
              updateCartBadge();
              renderCart();
              validateForm();
            }

            function setShipping(type) {
              shippingType = type;
              document.getElementById('ship-standard-btn').classList.toggle('active', type === 'standard');
              document.getElementById('ship-express-btn').classList.toggle('active', type === 'express');
              renderCart();
              validateForm();
            }

            function updateCartBadge() {
              const count = cart.reduce((acc, i) => acc + i.qty, 0);
              document.getElementById('cart-count').innerText = count;
            }

            function renderCatalog() {
              const q = (document.getElementById('search-input').value || '').trim().toLowerCase();
              let items = PRODUCTS.filter(p => {
                const catOk = activeCategory === 'all' || p.category === activeCategory;
                const wishOk = !onlyWishlist || wishlist.has(p.id);
                const textOk = !q || p.name.toLowerCase().includes(q) || p.category.toLowerCase().includes(q) || p.categoryAr.includes(q);
                return catOk && wishOk && textOk;
              });
              if (activeSort === 'low') items = [...items].sort((a, b) => a.price - b.price);
              else if (activeSort === 'high') items = [...items].sort((a, b) => b.price - a.price);

              document.getElementById('visible-count-label').innerText = items.length + ' منتجات معروضة / ' + items.length + ' products';
              document.getElementById('status-filter-text').innerText =
                'الفئة: ' + (onlyWishlist ? 'المفضلة / Wishlist' : activeCategory) + ' | الترتيب: ' + activeSort;

              const container = document.getElementById('catalog');
              if (items.length === 0) {
                container.innerHTML = '<div class="card"><div class="muted">لا توجد منتجات مطابقة للتصفية الحالية.</div><button class="primary-btn" onclick="resetFilters()">عرض كل المنتجات / Show All</button></div>';
                return;
              }
              container.innerHTML = items.map(p => {
                const wished = wishlist.has(p.id);
                const shortTitle = p.name.split('|')[1]?.trim() || p.name;
                return '<div class="card product" data-id="' + p.id + '">' +
                  '<div class="card-top">' +
                    '<span class="badge">' + p.category + ' • ' + p.categoryAr + '</span>' +
                    '<span class="price">$' + p.price + '</span>' +
                  '</div>' +
                  '<h3 style="margin:4px 0;">' + p.name + '</h3>' +
                  '<p class="muted">' + p.desc + '</p>' +
                  '<div class="card-actions">' +
                    '<button id="add-' + p.id + '-btn" class="primary-btn" onclick="addToCart(\'' + p.id + '\')">أضف للسلة / Add ' + shortTitle + '</button>' +
                    '<button id="wish-' + p.id + '-btn" class="ghost-btn" onclick="toggleWishlistItem(\'' + p.id + '\')">' + (wished ? '✓ في المفضلة / Saved' : '♡ حفظ للمفضلة / Save') + '</button>' +
                  '</div>' +
                '</div>';
              }).join('');
            }

            function toggleCart() {
              const panel = document.getElementById('checkout-panel');
              panel.classList.toggle('hidden');
              renderCart();
              validateForm();
            }

            function computeTotals() {
              const subtotal = cart.reduce((acc, i) => acc + i.price * i.qty, 0);
              const promo = (document.getElementById('promo-code').value || '').trim().toUpperCase();
              const shipFee = (cart.length > 0 && shippingType === 'express') ? 15 : 0;
              let discount = 0;
              let promoMsg = '';
              if (promo === 'MURSHED20') {
                discount = Math.round(subtotal * 0.20);
                promoMsg = 'تم تطبيق خصم 20% (MURSHED20): -$' + discount;
              } else if (promo === 'PRO30') {
                if (subtotal >= 150) {
                  discount = Math.round(subtotal * 0.30);
                  promoMsg = 'تم تطبيق خصم المحترفين 30% (PRO30): -$' + discount;
                } else {
                  promoMsg = 'كود PRO30 يتطلب سلة بقيمة $150 فأكثر (الحالي $' + subtotal + ').';
                }
              } else if (promo.length > 0) {
                promoMsg = 'كود غير معروف — جرّب MURSHED20 أو PRO30.';
              }
              const total = Math.max(0, subtotal - discount + shipFee);
              return { subtotal, discount, shipFee, total, promoMsg };
            }

            function renderCart() {
              const listEl = document.getElementById('cart-items-list');
              const totalEl = document.getElementById('total-line');
              const promoEl = document.getElementById('promo-feedback');
              const statusSub = document.getElementById('status-subtotal-text');

              if (cart.length === 0) {
                listEl.innerHTML = '<div class="muted">السلة فارغة / Cart is empty</div>';
                totalEl.innerText = 'الإجمالي / Total: $0';
                promoEl.innerText = '';
                statusSub.innerText = 'إجمالي السلة: $0';
                return;
              }

              listEl.innerHTML = cart.map(i =>
                '<div class="cart-line">' +
                  '<div style="flex:1;"><strong>' + i.name + '</strong><br/><span class="muted">$' + i.price + ' × ' + i.qty + ' = $' + (i.price * i.qty) + '</span></div>' +
                  '<div class="qty-controls">' +
                    '<button id="qty-dec-' + i.id + '" class="qty-btn" onclick="changeQty(\'' + i.id + '\', -1)">-</button>' +
                    '<span id="qty-val-' + i.id + '">' + i.qty + '</span>' +
                    '<button id="qty-inc-' + i.id + '" class="qty-btn" onclick="changeQty(\'' + i.id + '\', 1)">+</button>' +
                    '<button id="remove-' + i.id + '" class="danger-btn" onclick="removeFromCart(\'' + i.id + '\')">حذف</button>' +
                  '</div>' +
                '</div>'
              ).join('');

              const t = computeTotals();
              promoEl.innerText = t.promoMsg;
              totalEl.innerText = 'الإجمالي / Total: $' + t.total + (t.shipFee > 0 ? ' (يشمل شحن سريع $15)' : '');
              statusSub.innerText = 'إجمالي السلة: $' + t.total;
            }

            function validateForm() {
              renderCart();
              const name = document.getElementById('customer-name').value.trim();
              const addr = document.getElementById('customer-address').value.trim();
              const btn = document.getElementById('submit-order-btn');
              const hint = document.getElementById('form-validation-hint');
              const missing = [];
              if (cart.length === 0) missing.push('إضافة منتج للسلة / Add product');
              if (name.length <= 1) missing.push('الاسم / Full Name');
              if (addr.length <= 1) missing.push('العنوان / Address');
              btn.disabled = missing.length > 0;
              if (missing.length > 0) {
                hint.classList.remove('hidden');
                hint.innerText = 'المطلوب لإتمام الطلب: ' + missing.join(' • ');
              } else {
                hint.classList.add('hidden');
              }
            }

            function submitOrder() {
              const name = document.getElementById('customer-name').value.trim();
              const addr = document.getElementById('customer-address').value.trim();
              const t = computeTotals();
              const summary = 'العميل: ' + name + ' | العنوان: ' + addr + ' | المنتجات: ' + cart.map(i => i.qty + 'x ' + i.id).join(', ') + ' | الإجمالي: $' + t.total;
              sessionOrders.unshift({ id: sessionOrders.length + 1, summary });
              document.getElementById('checkout-panel').classList.add('hidden');
              const conf = document.getElementById('order-confirmation');
              conf.classList.remove('hidden');
              document.getElementById('confirmation-details').innerText = summary;
              renderOrderHistory();
            }

            function renderOrderHistory() {
              const sec = document.getElementById('order-history-section');
              if (sessionOrders.length === 0) {
                sec.classList.add('hidden');
                return;
              }
              sec.classList.remove('hidden');
              document.getElementById('orders-count').innerText = sessionOrders.length;
              document.getElementById('order-history-list').innerHTML = sessionOrders.map(o =>
                '<div class="cart-line"><span><strong>#' + o.id + '</strong> ' + o.summary + '</span></div>'
              ).join('');
            }

            function resetStore() {
              cart = [];
              updateCartBadge();
              document.getElementById('customer-name').value = '';
              document.getElementById('customer-address').value = '';
              document.getElementById('promo-code').value = '';
              setShipping('standard');
              document.getElementById('order-confirmation').classList.add('hidden');
              document.getElementById('checkout-panel').classList.add('hidden');
              resetFilters();
              renderCart();
              validateForm();
            }

            renderCatalog();
            validateForm();
          </script>
        </body>
        </html>
    """.trimIndent()
}
