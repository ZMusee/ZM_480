// Main left-side menu/nav panel
import NavButton from "./NavButton.jsx"
import ProjectAccordion from "./ProjectAccordion.jsx"
import {LogOutIcon} from "lucide-react"

export function SidePanel() {
    return (
      <nav className="mt-6 bg-blueLight w-[222px] fixed top-0 bottom-0 pt-16">
            <ul className="flex flex-col h-full ml-4">
                <li className="flex flex-col mr-4">
                    <NavButton href="/">
                            My Tasks
                    </NavButton>
                </li>
                <li className="flex flex-col mr-4">
                    <NavButton href="/deleted">                      
                            Recently Deleted
                    </NavButton>
                </li>
                <hr className="w-[11rem] ml-1 my-3 border-neutral-400"/>
                <li>
                    <menu className="flex flex-col items-start">
                        <ProjectAccordion/>
                    </menu>
                </li>
                <li className="flex flex-col mb-8 mt-auto ">
                    <NavButton href={`${import.meta.env.VITE_AUTH_ROOT}/auth/logout`}>Log out
                        <LogOutIcon className="resize scale-50 translate-x-24"></LogOutIcon>
                    </NavButton>
                </li>
            </ul>
        </nav>
    )
}
