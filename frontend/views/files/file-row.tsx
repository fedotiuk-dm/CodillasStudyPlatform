import { Badge } from "@/components/ui/badge";
import { TableCell, TableRow } from "@/components/ui/table";
import type { StoredFileResponse } from "@/lib/api/files/model";

import { FileRowActions } from "./file-row-actions";

function humanSize(bytes: number) {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`;
}

/** One stored file: name, reference-type badge, human-readable size, and download/delete actions. */
export function FileRow({ file }: { file: StoredFileResponse }) {
  return (
    <TableRow>
      <TableCell className="font-medium">{file.originalFilename}</TableCell>
      <TableCell>
        <Badge variant="outline">{file.referenceType ?? "—"}</Badge>
      </TableCell>
      <TableCell className="text-muted-foreground">{humanSize(file.fileSize)}</TableCell>
      <TableCell>
        <FileRowActions file={file} />
      </TableCell>
    </TableRow>
  );
}
