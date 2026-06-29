"use client";

import { useTranslations } from "next-intl";
import { useState } from "react";

import { DataState } from "@/components/shared/data-state";
import { PageHeader } from "@/components/shared/page-header";
import { Card, CardContent } from "@/components/ui/card";
import { Table, TableBody, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import type { StoredFileResponse } from "@/lib/api/files/model";
import { FileReferenceType } from "@/lib/api/files/model";

import { FileRow } from "./file-row";
import { FileUploadControl } from "./file-upload-control";

export function FilesView() {
  const t = useTranslations("files");
  // ponytail: session-local list — the Files API has no list endpoint, only by-id.
  const [files, setFiles] = useState<StoredFileResponse[]>([]);
  const [referenceType, setReferenceType] = useState<FileReferenceType>(FileReferenceType.MATERIAL);

  return (
    <>
      <PageHeader
        title={t("title")}
        description={t("description")}
        action={
          <FileUploadControl
            referenceType={referenceType}
            onReferenceTypeChange={setReferenceType}
            onUploaded={(stored) => setFiles((prev) => [stored, ...prev])}
          />
        }
      />

      <Card>
        <CardContent className="pt-6">
          <DataState isLoading={false} isError={false} isEmpty={files.length === 0}>
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>{t("name")}</TableHead>
                  <TableHead>{t("type")}</TableHead>
                  <TableHead>{t("size")}</TableHead>
                  <TableHead className="text-right">{t("actions")}</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {files.map((f) => (
                  <FileRow
                    key={f.id}
                    file={f}
                    onDeleted={(id) => setFiles((prev) => prev.filter((x) => x.id !== id))}
                  />
                ))}
              </TableBody>
            </Table>
          </DataState>
        </CardContent>
      </Card>
    </>
  );
}
