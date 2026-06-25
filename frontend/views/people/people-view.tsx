"use client";

import { Search } from "lucide-react";
import { useState } from "react";

import { DataState } from "@/components/shared/data-state";
import { PageHeader } from "@/components/shared/page-header";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { useListProfiles } from "@/lib/api/user/user/user";
import { useDebounced } from "@/lib/hooks/use-debounced";

export function PeopleView() {
  const [query, setQuery] = useState("");
  const debounced = useDebounced(query.trim());

  const { data, isLoading, isError } = useListProfiles({ q: debounced || undefined, size: 50 });
  const people = data?.content ?? [];

  return (
    <>
      <PageHeader title="People" description="Local profiles of everyone in the system." />

      <div className="relative mb-4 max-w-sm">
        <Search className="-translate-y-1/2 absolute top-1/2 left-2.5 size-4 text-muted-foreground" />
        <Input
          className="pl-8"
          placeholder="Search by name…"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
        />
      </div>

      <Card>
        <CardContent className="pt-6">
          <DataState
            isLoading={isLoading}
            isError={isError}
            isEmpty={people.length === 0}
            emptyMessage="No profiles found."
          >
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Name</TableHead>
                  <TableHead>Bio</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {people.map((p) => (
                  <TableRow key={p.userId}>
                    <TableCell className="font-medium">{p.displayName}</TableCell>
                    <TableCell className="text-muted-foreground">{p.bio ?? "—"}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </DataState>
        </CardContent>
      </Card>
    </>
  );
}
