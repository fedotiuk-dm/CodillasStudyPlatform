"use client";

import { useTranslations } from "next-intl";
import { useState } from "react";

import { DataState } from "@/components/shared/data-state";
import { PageHeader } from "@/components/shared/page-header";
import { Card, CardContent } from "@/components/ui/card";
import { Table, TableBody, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { useListMyFiles } from "@/lib/api/files/files/files";
import { FileReferenceType } from "@/lib/api/files/model";

import { FileRow } from "./file-row";
import { FileUploadControl } from "./file-upload-control";

export function FilesView() {
  const t = useTranslations("files");
  const [referenceType, setReferenceType] = useState<FileReferenceType>(FileReferenceType.MATERIAL);
  const { data, isLoading, isError, refetch } = useListMyFiles();

  const files = data?.content ?? [];

  return (
    <>
      <PageHeader
        title={t("title")}
        description={t("description")}
        action={
          <FileUploadControl
            referenceType={referenceType}
            onReferenceTypeChange={setReferenceType}
          />
        }
      />

      <Card>
        <CardContent className="pt-6">
          <DataState
            isLoading={isLoading}
            isError={isError}
            isEmpty={files.length === 0}
            onRetry={refetch}
          >
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
                  <FileRow key={f.id} file={f} />
                ))}
              </TableBody>
            </Table>
          </DataState>
        </CardContent>
      </Card>
    </>
  );
}
