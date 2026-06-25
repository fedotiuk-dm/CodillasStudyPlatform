"use client";

import { useQueryClient } from "@tanstack/react-query";
import { Trash2 } from "lucide-react";
import { useFieldArray, useForm } from "react-hook-form";
import { toast } from "sonner";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Separator } from "@/components/ui/separator";
import {
  getGetTestQueryKey,
  useAddQuestion,
  useGetTest,
} from "@/lib/api/assessment/assessment/assessment";
import { QuestionType } from "@/lib/api/assessment/model";

type FormValues = {
  type: QuestionType;
  prompt: string;
  points: number;
  options: { text: string; correct: boolean }[];
};

const NEEDS_OPTIONS: QuestionType[] = [
  QuestionType.SINGLE_CHOICE,
  QuestionType.MULTIPLE_CHOICE,
  QuestionType.TRUE_FALSE,
];

export function ManageQuestionsDialog({
  testId,
  open,
  onOpenChange,
}: {
  testId: string;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}) {
  const queryClient = useQueryClient();
  const { data: test } = useGetTest(testId, { query: { enabled: open } });
  const addQuestion = useAddQuestion();

  const form = useForm<FormValues>({
    defaultValues: {
      type: QuestionType.SINGLE_CHOICE,
      prompt: "",
      points: 1,
      options: [
        { text: "", correct: true },
        { text: "", correct: false },
      ],
    },
  });
  const { fields, append, remove } = useFieldArray({ control: form.control, name: "options" });
  const type = form.watch("type");
  const needsOptions = NEEDS_OPTIONS.includes(type);

  function onSubmit(values: FormValues) {
    addQuestion.mutate(
      {
        testId,
        data: {
          type: values.type,
          prompt: values.prompt,
          points: Number(values.points),
          options: needsOptions
            ? values.options.filter((o) => o.text.trim()).map((o) => ({ ...o }))
            : undefined,
        },
      },
      {
        onSuccess: () => {
          toast.success("Question added");
          queryClient.invalidateQueries({ queryKey: getGetTestQueryKey(testId) });
          form.reset();
        },
        onError: () => toast.error("Could not add the question"),
      },
    );
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[85vh] overflow-y-auto sm:max-w-2xl">
        <DialogHeader>
          <DialogTitle>{test?.title ?? "Questions"}</DialogTitle>
          <DialogDescription>Add questions to this test before publishing.</DialogDescription>
        </DialogHeader>

        {test && test.questions.length > 0 && (
          <ol className="grid gap-2">
            {test.questions.map((q, i) => (
              <li key={q.id} className="flex items-start gap-2 text-sm">
                <span className="text-muted-foreground">{i + 1}.</span>
                <span className="flex-1">{q.prompt}</span>
                <Badge variant="outline">{q.type}</Badge>
                <span className="text-muted-foreground">{q.points} pt</span>
              </li>
            ))}
          </ol>
        )}

        <Separator />

        <Form {...form}>
          <form onSubmit={form.handleSubmit(onSubmit)} className="grid gap-4">
            <FormField
              control={form.control}
              name="type"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Type</FormLabel>
                  <Select onValueChange={field.onChange} value={field.value}>
                    <FormControl>
                      <SelectTrigger>
                        <SelectValue />
                      </SelectTrigger>
                    </FormControl>
                    <SelectContent>
                      {Object.values(QuestionType).map((t) => (
                        <SelectItem key={t} value={t}>
                          {t}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="prompt"
              rules={{ required: "Prompt is required" }}
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Prompt</FormLabel>
                  <FormControl>
                    <Input placeholder="The question…" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="points"
              rules={{ required: true, min: { value: 1, message: "At least 1 point" } }}
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Points</FormLabel>
                  <FormControl>
                    <Input type="number" min={1} {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            {needsOptions && (
              <div className="grid gap-2">
                <FormLabel>Options (tick the correct ones)</FormLabel>
                {fields.map((f, i) => (
                  <div key={f.id} className="flex items-center gap-2">
                    <FormField
                      control={form.control}
                      name={`options.${i}.correct`}
                      render={({ field }) => (
                        <Checkbox checked={field.value} onCheckedChange={field.onChange} />
                      )}
                    />
                    <Input
                      className="flex-1"
                      placeholder={`Option ${i + 1}`}
                      {...form.register(`options.${i}.text`)}
                    />
                    <Button type="button" variant="ghost" size="icon" onClick={() => remove(i)}>
                      <Trash2 className="size-4" />
                    </Button>
                  </div>
                ))}
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  className="justify-self-start"
                  onClick={() => append({ text: "", correct: false })}
                >
                  Add option
                </Button>
              </div>
            )}

            <Button type="submit" disabled={addQuestion.isPending}>
              {addQuestion.isPending ? "Adding…" : "Add question"}
            </Button>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  );
}
