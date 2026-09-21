import { RoleGate } from "@/components/auth/role-gate";
import { Role } from "@/lib/constants";
import { AdminView } from "@/views/admin/admin-view";

export default function AdminPage() {
  return (
    <RoleGate roles={[Role.ADMIN]}>
      <AdminView />
    </RoleGate>
  );
}
