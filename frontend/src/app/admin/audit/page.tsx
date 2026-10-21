"use client";

import { useRouter } from "next/navigation";
import { useCallback, useEffect, useRef, useState } from "react";
import { apiFetch, logout } from "@/lib/api";
import {
  AUDIT_ACTION_LABELS,
  AUDIT_TARGET_LABELS,
  ROLE_LABELS,
  type AuditEntry,
} from "@/lib/admin-types";
import { adminRedirect } from "@/lib/two-factor";
import { AppHeader } from "@/components/ui/AppHeader";
import { Section, SectionHeader } from "@/components/ui/Card";
import { Field, Select } from "@/components/ui/Field";
import { QuietButton, SecondaryLink } from "@/components/ui/SecondaryButton";

const PAGE_SIZE = 25;

interface Filters {
  action: string;
  targetType: string;
}

/**
 * Read-only view of the audit log. Shows only what GET /api/admin/audit returns — ids, roles,
 * actions, IPs — never names or any other child/parent personal data.
 */
export default function AdminAuditPage() {
  const router = useRouter();
  const [filters, setFilters] = useState<Filters>({ action: "", targetType: "" });
  const [offset, setOffset] = useState(0);
  const [entries, setEntries] = useState<AuditEntry[] | null>(null);
  const [hasMore, setHasMore] = useState(false);
  const [loadError, setLoadError] = useState<string | null>(null);
  // Fast filter/page switching can resolve out of order; only the latest request may render.
  const requestSeq = useRef(0);

  const load = useCallback(
    (nextFilters: Filters, nextOffset: number) => {
      const params = new URLSearchParams({ limit: String(PAGE_SIZE + 1), offset: String(nextOffset) });
      if (nextFilters.action) params.set("action", nextFilters.action);
      if (nextFilters.targetType) params.set("targetType", nextFilters.targetType);
      const seq = ++requestSeq.current;
      apiFetch<AuditEntry[]>(`/admin/audit?${params}`)
        .then((data) => {
          if (seq !== requestSeq.current) return;
          // One extra row tells us whether a next page exists without a separate count query.
          setHasMore(data.length > PAGE_SIZE);
          setEntries(data.slice(0, PAGE_SIZE));
          setLoadError(null);
        })
        .catch((err) => {
          if (seq !== requestSeq.current) return;
          const target = adminRedirect(err);
          if (target) {
            router.replace(target);
            return;
          }
          setLoadError("Не вдалося завантажити журнал аудиту.");
        });
    },
    [router],
  );

  useEffect(() => {
    load({ action: "", targetType: "" }, 0);
  }, [load]);

  function applyFilters(next: Filters) {
    setFilters(next);
    setOffset(0);
    setEntries(null);
    load(next, 0);
  }

  function goTo(nextOffset: number) {
    setOffset(nextOffset);
    setEntries(null);
    load(filters, nextOffset);
  }

  async function onLogout() {
    await logout();
    router.replace("/login");
  }

  return (
    <>
      <AppHeader context="Адміністратор" onLogout={onLogout} />
      <main className="flex flex-1 justify-center px-6 pt-10 pb-24">
        <div className="flex w-full max-w-300 flex-col gap-7">
          <div className="flex flex-col gap-2">
            <SecondaryLink href="/admin">← Адмін-кабінет</SecondaryLink>
            <h1 className="text-[32px] leading-[1.15] font-medium tracking-[-0.02em]">Аудит</h1>
            <p className="text-ink-muted text-base leading-relaxed">
              Хто, що і коли робив з даними та контентом. Лише ідентифікатори — без імен і персональних даних.
            </p>
          </div>

          <Section>
            <SectionHeader title="Журнал дій" />
            <div className="border-line-soft grid grid-cols-1 gap-4 border-b px-6 py-4.5 md:grid-cols-2">
              <Field label="Дія">
                {({ id }) => (
                  <Select
                    id={id}
                    value={filters.action}
                    onChange={(e) => applyFilters({ ...filters, action: e.target.value })}
                  >
                    <option value="">Усі дії</option>
                    {Object.entries(AUDIT_ACTION_LABELS).map(([value, label]) => (
                      <option key={value} value={value}>
                        {label}
                      </option>
                    ))}
                  </Select>
                )}
              </Field>
              <Field label="Тип цілі">
                {({ id }) => (
                  <Select
                    id={id}
                    value={filters.targetType}
                    onChange={(e) => applyFilters({ ...filters, targetType: e.target.value })}
                  >
                    <option value="">Усі типи</option>
                    {Object.entries(AUDIT_TARGET_LABELS).map(([value, label]) => (
                      <option key={value} value={value}>
                        {label}
                      </option>
                    ))}
                  </Select>
                )}
              </Field>
            </div>

            {loadError ? <p className="text-danger px-6 py-5 text-sm">{loadError}</p> : null}
            {!entries && !loadError ? <p className="text-ink-muted px-6 py-5 text-sm">Завантаження…</p> : null}
            {entries && entries.length === 0 ? (
              <p className="text-ink-muted px-6 py-5 text-sm">Записів не знайдено.</p>
            ) : null}

            {entries && entries.length > 0 ? (
              <div className="overflow-x-auto">
                <table className="w-full min-w-[760px] text-left text-sm">
                  <thead className="bg-row-hover text-ink-faint font-mono text-xs tracking-[0.08em] uppercase">
                    <tr>
                      <th className="px-6 py-3 font-normal">Час</th>
                      <th className="px-3 py-3 font-normal">Дія</th>
                      <th className="px-3 py-3 font-normal">Роль</th>
                      <th className="px-3 py-3 font-normal">Актор</th>
                      <th className="px-3 py-3 font-normal">Ціль</th>
                      <th className="px-6 py-3 font-normal">IP</th>
                    </tr>
                  </thead>
                  <tbody>
                    {entries.map((entry) => (
                      <tr key={entry.id} className="border-line-row border-t">
                        <td className="px-6 py-3 whitespace-nowrap">
                          {new Date(entry.createdAt).toLocaleString("uk-UA")}
                        </td>
                        <td className="px-3 py-3">{AUDIT_ACTION_LABELS[entry.action] ?? entry.action}</td>
                        <td className="px-3 py-3">{ROLE_LABELS[entry.actorRole] ?? entry.actorRole}</td>
                        <td className="text-ink-icon px-3 py-3 font-mono" title={entry.actorId ?? undefined}>
                          {shortId(entry.actorId)}
                        </td>
                        <td className="px-3 py-3">
                          <span>{AUDIT_TARGET_LABELS[entry.targetType] ?? entry.targetType}</span>{" "}
                          <span className="text-ink-icon font-mono" title={entry.targetId ?? undefined}>
                            {shortId(entry.targetId)}
                          </span>
                        </td>
                        <td className="text-ink-icon px-6 py-3 font-mono">{entry.clientIp ?? "—"}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            ) : null}

            <div className="border-line-soft flex items-center justify-between gap-3 border-t px-6 py-4">
              <QuietButton type="button" disabled={offset === 0} onClick={() => goTo(Math.max(0, offset - PAGE_SIZE))}>
                ← Попередні
              </QuietButton>
              <span className="text-ink-icon text-sm">
                {entries && entries.length > 0 ? `${offset + 1}–${offset + entries.length}` : ""}
              </span>
              <QuietButton type="button" disabled={!hasMore} onClick={() => goTo(offset + PAGE_SIZE)}>
                Наступні →
              </QuietButton>
            </div>
          </Section>
        </div>
      </main>
    </>
  );
}

function shortId(id: string | null): string {
  return id ? id.slice(0, 8) : "—";
}
