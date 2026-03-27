document.addEventListener("DOMContentLoaded", () => {
  const form = document.querySelector("[data-contact-form]");
  if (!form) return;

  const member = (() => {
    try {
      return JSON.parse(localStorage.getItem("hamu_member") || "null");
    } catch (error) {
      return null;
    }
  })();

  if (member?.loggedIn) {
    const nameInput = form.querySelector('input[name="name"]');
    if (nameInput && !nameInput.value) {
      nameInput.value = member.name || "";
    }
  }

  const endpoint = form.dataset.endpoint || "http://localhost:8080/api/inquiries";
  const statusBox = document.querySelector("[data-contact-status]");
  const submitButton = form.querySelector('button[type="submit"]');

  const setStatus = (message, type = "info") => {
    if (!statusBox) return;
    statusBox.textContent = message;
    statusBox.dataset.state = type;
  };

  form.addEventListener("submit", async (event) => {
    event.preventDefault();

    const formData = new FormData(form);
    const payload = {
      name: String(formData.get("name") || "").trim(),
      email: String(member?.loggedIn ? (member.email || "") : "").trim(),
      subject: String(formData.get("subject") || "").trim(),
      message: String(formData.get("message") || "").trim()
    };

    if (!member?.loggedIn || !payload.email) {
      const message = "フォーム送信にはログインが必要です。";
      setStatus(message, "error");
      alert(message);
      return;
    }

    if (!payload.name || !payload.subject || !payload.message) {
      const message = "未入力の項目があります。すべて入力してください。";
      setStatus(message, "error");
      alert(message);
      return;
    }

    submitButton.disabled = true;
    setStatus("送信中です。少しお待ちください。");

    try {
      const response = await fetch(endpoint, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload)
      });

      const data = await response.json().catch(() => ({}));
      if (!response.ok) {
        const message = data.message || "送信に失敗しました。入力内容を確認してください。";
        setStatus(message, "error");
        alert(message);
        return;
      }

      const message = data.message || "お問い合わせを受け付けました。";
      setStatus(message, "success");
      alert(message);
      form.reset();

      const nameInput = form.querySelector('input[name="name"]');
      if (nameInput) {
        nameInput.value = member.name || "";
      }
    } catch (error) {
      const message = "送信できませんでした。バックエンドが起動中か確認してください。";
      setStatus(message, "error");
      alert(message);
    } finally {
      submitButton.disabled = false;
    }
  });
});
