import type { Metadata } from "next";
import Link from "next/link";
import type { ReactNode } from "react";
import { AppHeader } from "@/components/ui/AppHeader";

export const metadata: Metadata = {
  title: "Умови використання — Безпечний Простір",
};

/**
 * Terms of use — a STRUCTURED DRAFT for a lawyer, like /privacy (QA1-012). Every factual statement
 * describes what the platform actually does; the legal wording, the provider's details, liability
 * and governing law are deliberately left as TODO, not invented.
 */
export default function TermsPage() {
  return (
    <>
      <AppHeader context="Інформація" />
      <main className="flex flex-1 justify-center px-6 pt-8 pb-24">
        <article className="flex w-full max-w-200 flex-col gap-7 leading-relaxed">
          <div className="border-notice-line bg-notice-surface text-notice-title rounded-xl border px-5 py-4 font-medium">
            ЧЕРНЕТКА — потребує перевірки юристом. Розділи з позначкою TODO ще не заповнені й не є
            юридичними зобов&apos;язаннями.
          </div>

          <h1 className="text-[32px] leading-[1.15] font-medium tracking-[-0.02em]">Умови використання</h1>

          <Part title="1. Хто надає сервіс">
            <Todo>Назва юридичної особи / ФОП, реєстраційні дані, адреса, контакт.</Todo>
          </Part>

          <Part title="2. Що це за сервіс">
            <p>
              «Безпечний Простір» — навчальна платформа для дітей 3–6 років про безпеку (правила дорожнього руху, номери
              екстрених служб). Зараз вона працює в пілотному режимі: можливості змінюються, можливі перерви в роботі.
            </p>
            <Todo>Чи сервіс безоплатний на час пілоту; що буде після нього.</Todo>
          </Part>

          <Part title="3. Акаунти">
            <ul className="list-disc pl-6">
              <li>Акаунт створює лише повнолітній: батьки (або інший законний представник), вчитель за запрошенням адміністратора, адміністратор.</li>
              <li>Дитина власного акаунта не має: вона навчається в дитячому режимі, який вмикає дорослий зі свого акаунта; вийти з нього можна лише з паролем дорослого.</li>
              <li>Профіль дитини можна створити лише після підтвердження email і надання згоди батьків.</li>
              <li>Дорослий відповідає за збереження свого пароля; у кабінеті можна змінити пароль і вийти на всіх пристроях.</li>
            </ul>
            <Todo>Вимоги до законного представника; що робити, якщо акаунт використано без дозволу.</Todo>
          </Part>

          <Part title="4. Навчальний контент">
            <ul className="list-disc pl-6">
              <li>Уроки створюються за допомогою штучного інтелекту й автоматично перевіряються, але дитина бачить урок лише після того, як його переглянула й схвалила людина.</li>
              <li>Номери екстрених служб в уроках беруться лише з перевіреної таблиці (101, 102, 103, 104, 112).</li>
              <li>Уроки не замінюють дорослого: вони вчать звертатися до дорослого по допомогу.</li>
            </ul>
            <Todo>Застереження щодо відповідальності за зміст; як повідомити про помилку в уроці.</Todo>
          </Part>

          <Part title="5. Правила користування">
            <ul className="list-disc pl-6">
              <li>Вчитель бачить лише імена й результати дітей своїх груп; батьки — лише своїх дітей.</li>
              <li>Заборонено намагатися отримати доступ до чужих даних, обходити обмеження спроб входу чи навантажувати сервіс автоматично.</li>
            </ul>
            <Todo>Наслідки порушень (блокування акаунта адміністратором).</Todo>
          </Part>

          <Part title="6. Дані та їх видалення">
            <p>
              Які дані ми зберігаємо, скільки і як їх завантажити чи видалити — у{" "}
              <Link href="/privacy">Політиці конфіденційності</Link>. Видалити профіль дитини чи власний акаунт можна в
              кабінеті, без звернення до підтримки.
            </p>
          </Part>

          <Part title="7. Відповідальність">
            <Todo>Обмеження відповідальності, гарантії доступності сервісу, форс-мажор.</Todo>
          </Part>

          <Part title="8. Зміни умов">
            <Todo>Як і з яким попередженням повідомляємо про зміни; застосовне право і порядок вирішення спорів.</Todo>
          </Part>

          <Part title="9. Контакти">
            <Todo>Email / адреса для звернень.</Todo>
          </Part>

          <Link href="/login" className="text-sm">
            ← До входу
          </Link>
        </article>
      </main>
    </>
  );
}

function Part({ title, children }: { title: string; children: ReactNode }) {
  return (
    <section className="flex flex-col gap-3">
      <h2 className="text-[20px] font-medium">{title}</h2>
      {children}
    </section>
  );
}

function Todo({ children }: { children: ReactNode }) {
  return (
    <p className="border-dev-mark text-ink-soft rounded-lg border border-dashed px-3 py-2 text-sm">
      <b>TODO (юрист):</b> {children}
    </p>
  );
}
